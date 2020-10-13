package uk.gov.ons.census.fwmt.fulfilment.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.data.fulfillment.dto.PauseOutcome;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.fulfilment.lookup.ChannelLookup;
import uk.gov.ons.census.fwmt.fulfilment.service.FulfilmentService;

@Slf4j
@Component
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

  @RabbitListener
  public void receiveMessage(Object fulfillmentEvent) throws GatewayException, JsonProcessingException {
    String channelId;
    String channelSent;

    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json = ow.writeValueAsString(fulfillmentEvent);
    PauseOutcome pauseOutcome = jsonMapper.readValue(json, PauseOutcome.class);

    channelSent = pauseOutcome.getEvent().getChannel();

    channelId = channelLookup.getLookup(channelSent);
    if (channelId != null) {
      eventManager.triggerEvent(pauseOutcome.getPayload().getFulfilmentRequest().getCaseId(), RECEIVED_FULFILMENT, "Test");
      fulfilmentService.processPauseCase(pauseOutcome);
    } else {
      eventManager.triggerErrorEvent(this.getClass(), "Could not find a matching channel for the fulfilment pause request",
          pauseOutcome.getPayload().getFulfilmentRequest().getCaseId(), "Channel: " +
              channelSent + "Product code: " + pauseOutcome.getPayload().getFulfilmentRequest().getFulfilmentCode());
      throw new AmqpRejectAndDontRequeueException(null, true, null);
    }
  }
}
