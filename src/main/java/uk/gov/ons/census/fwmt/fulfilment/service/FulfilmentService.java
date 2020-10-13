package uk.gov.ons.census.fwmt.fulfilment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fwmt.common.data.fulfillment.dto.PauseOutcome;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.common.rm.dto.ActionInstructionType;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtActionInstruction;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.fulfilment.data.GatewayCache;
import uk.gov.ons.census.fwmt.fulfilment.lookup.PauseRulesLookup;
import uk.gov.ons.census.fwmt.fulfilment.rabbit.MessagePublisher;

@Slf4j
@Service
public class FulfilmentService {

  private static final String PAUSE_PROCESSED_AND_SENT = "PAUSE_PROCESSED_AND_SENT";
  @Autowired
  private PauseRulesLookup pauseRulesLookup;

  @Autowired
  private GatewayCacheService cacheService;

  @Autowired
  private GatewayEventManager eventManager;

  @Autowired
  private MessagePublisher messagePublisher;

  public void processPauseCase(PauseOutcome pauseRequest) throws GatewayException {
    FwmtActionInstruction pauseActionInstruction = new FwmtActionInstruction();
    String caseId = "";
    String pauseRule;
    String productCode = pauseRequest.getPayload().getFulfilmentRequest().getFulfilmentCode();

    final GatewayCache caseCache = cacheService.getByIdAndTypeAndExists(pauseRequest.getPayload().getFulfilmentRequest()
        .getCaseId(), 10, true);
    final GatewayCache indCache = cacheService.getByIndividualCaseIdAndTypeAndExists(pauseRequest.getPayload().getFulfilmentRequest()
        .getIndividualCaseId(), 10, true);
    GatewayCache cache;

    if (caseCache == null && indCache == null){
      eventManager.triggerErrorEvent(this.getClass(), "Could not find an existing record",
          caseId, "ROUTING_FAILED");
      throw new AmqpRejectAndDontRequeueException(null, true, null);
      //      throw new GatewayException(GatewayException.Fault.VALIDATION_FAILED, "Could not find an existing record",
//          String.valueOf(pauseRequest.getPayload().getFulfilmentRequest().getCaseId()));
    } else if (caseCache == null) {
      caseId = indCache.getIndividualCaseId();
    } else {
      caseId = caseCache.getCaseId();
    }

    pauseRule = pauseRulesLookup.getLookup(pauseRequest.getPayload().getFulfilmentRequest().getFulfilmentCode());

    if (pauseRule == null) {
      eventManager.triggerErrorEvent(this.getClass(), "Could not find a rule for the fulfilment request and product code.",
          String.valueOf(caseId), "Product code: " + productCode);
      throw new AmqpRejectAndDontRequeueException(null, true, null);
    } else {
      pauseActionInstruction.setActionInstruction(ActionInstructionType.PAUSE);
      pauseActionInstruction.setSurveyName("CENSUS");
      pauseActionInstruction.setAddressType("HH");
      pauseActionInstruction.setAddressLevel("U");
      pauseActionInstruction.setPauseCode(pauseRule);
      pauseActionInstruction.setCaseId(caseId);

      messagePublisher.pausePublish(pauseActionInstruction);

      eventManager.triggerEvent(pauseRequest.getPayload().getFulfilmentRequest().getCaseId(), PAUSE_PROCESSED_AND_SENT);
    }
  }
}
