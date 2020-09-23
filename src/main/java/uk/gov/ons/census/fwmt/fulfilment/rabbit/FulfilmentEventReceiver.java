package uk.gov.ons.census.fwmt.fulfilment.rabbit;

import lombok.extern.slf4j.Slf4j;
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

  public FulfilmentEventReceiver(FulfilmentService fulfilmentService, GatewayEventManager eventManager) {
    this.fulfilmentService = fulfilmentService;
    this.eventManager = eventManager;
  }

  public void receiveMessage(PauseOutcome fulfillmentEvent) throws GatewayException {
    String channelId;
    channelId = channelLookup.getLookup(fulfillmentEvent.getEvent().getChannel());

    if (channelId != null) {
      eventManager.triggerEvent(fulfillmentEvent.getPayload().getFulfilmentRequest().getCaseId(), RECEIVED_FULFILMENT, "Test");
      fulfilmentService.processPauseCase(fulfillmentEvent);
    } else {

      eventManager.triggerErrorEvent(this.getClass(), "Could not find a matching channel for the pause request",
          String.valueOf(fulfillmentEvent.getPayload().getFulfilmentRequest().getCaseId()), "ROUTING_FAILED");
      throw new GatewayException(GatewayException.Fault.VALIDATION_FAILED,
          "Could not find a matching channel for pause request", fulfillmentEvent);
    }
  }
}
