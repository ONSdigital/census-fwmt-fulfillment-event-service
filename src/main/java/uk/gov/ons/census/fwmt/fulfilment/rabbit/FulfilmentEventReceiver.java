package uk.gov.ons.census.fwmt.fulfilment.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.data.fulfillment.dto.PauseOutcome;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.fulfilment.lookup.ChannelLookup;
import uk.gov.ons.census.fwmt.fulfilment.service.FulfilmentService;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
@Component
@RabbitListener(queues = "${rabbitmq.exchange.queue}", containerFactory = "fulfilmentQueueListener")
public class FulfilmentEventReceiver {

  private static final String RECEIVED_FULFILMENT = "RECEIVED_FULFILMENT";
  @Autowired
  private FulfilmentService fulfilmentService;

  @Autowired
  private GatewayEventManager eventManager;


  @Autowired
  private ChannelLookup channelLookup;

  @Autowired
  private ObjectMapper jsonMapper;

  public FulfilmentEventReceiver(FulfilmentService fulfilmentService, GatewayEventManager eventManager) {
    this.fulfilmentService = fulfilmentService;
    this.eventManager = eventManager;
  }

  @RabbitHandler
  public void receiveMessage(Object fulfillmentEvent, @Header("timestamp") String timestamp) throws GatewayException, JsonProcessingException {
    long epochTimeStamp = Long.parseLong(timestamp);
    Instant receivedMessageTime = Instant.ofEpochMilli(epochTimeStamp);
    String channelId;
    String channelSent;
    Message convertToMessage = (Message) fulfillmentEvent;
    String pausePayload = new String(convertToMessage.getBody(), StandardCharsets.UTF_8);

    try {
      PauseOutcome pauseOutcome = jsonMapper.readValue(pausePayload, PauseOutcome.class);

      channelSent = pauseOutcome.getEvent().getChannel();

      channelId = channelLookup.getLookup(channelSent);
      if (channelId != null) {
        eventManager
            .triggerEvent(pauseOutcome.getPayload().getFulfilmentRequest().getCaseId(), RECEIVED_FULFILMENT, "Test");
        fulfilmentService.processPauseCase(pauseOutcome, receivedMessageTime);
      } else {
        eventManager
            .triggerErrorEvent(this.getClass(), "Could not find a matching channel for the fulfilment pause request",
                pauseOutcome.getPayload().getFulfilmentRequest().getCaseId(), "Channel: " +
                    channelSent + "Product code: " + pauseOutcome.getPayload().getFulfilmentRequest()
                    .getFulfilmentCode());
        throw new AmqpRejectAndDontRequeueException(null, true, null);
      }
    } catch (JsonProcessingException e) {
      eventManager
          .triggerErrorEvent(this.getClass(), "Unable to convert message to json",
              pausePayload, e.getMessage());
    }
  }
}
