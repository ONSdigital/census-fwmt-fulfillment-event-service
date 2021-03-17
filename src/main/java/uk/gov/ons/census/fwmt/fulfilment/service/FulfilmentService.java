package uk.gov.ons.census.fwmt.fulfilment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fwmt.common.data.fulfillment.dto.PauseOutcome;
import uk.gov.ons.census.fwmt.common.rm.dto.ActionInstructionType;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtActionInstruction;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.fulfilment.data.GatewayCache;
import uk.gov.ons.census.fwmt.fulfilment.lookup.PauseRulesLookup;
import uk.gov.ons.census.fwmt.fulfilment.rabbit.MessagePublisher;

import javax.transaction.Transactional;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

@Slf4j
@Service
@Transactional
public class FulfilmentService {

  private static final String PAUSE_PROCESSED_AND_SENT = "PAUSE_PROCESSED_AND_SENT";

  private static final String NO_RECORD_FOUND = "NO_RECORD_FOUND";

  private static final String CASE_ALREADY_CANCELLED = "CASE_ALREADY_CANCELLED";

  private static final String UNRECOGNISED_FULFILLMENT_CODE = "UNRECOGNISED_FULFILLMENT_CODE";

  @Autowired
  private PauseRulesLookup pauseRulesLookup;

  @Autowired
  private GatewayCacheService cacheService;

  @Autowired
  private GatewayEventManager eventManager;

  @Autowired
  private MessagePublisher messagePublisher;

  public void processPauseCase(PauseOutcome pauseRequest, Instant messageReceivedTime) {
    String caseId;
    String productCode = pauseRequest.getPayload().getFulfilmentRequest().getFulfilmentCode();

    final GatewayCache caseCache = cacheService.getByIdAndTypeAndExists(pauseRequest.getPayload().getFulfilmentRequest().getCaseId(),
        10, true);
    final GatewayCache indCache = cacheService.getByIndividualCaseIdAndTypeAndExists(pauseRequest.getPayload().getFulfilmentRequest().getIndividualCaseId(),
        10, true);

    if (caseCache == null && indCache == null) {
      caseId = pauseRequest.getPayload().getFulfilmentRequest().getCaseId();
      eventManager.triggerEvent(caseId, "Could not find an existing record or case is not a household", NO_RECORD_FOUND);
    } else if (caseCache == null) {
      caseId = indCache.individualCaseId;
      if ("CANCEL".equals(indCache.lastActionInstruction) || "CANCEL(HELD)".equals(indCache.lastActionInstruction)) {
        eventManager.triggerEvent(caseId, CASE_ALREADY_CANCELLED);
      } else {
        sendPause(pauseRequest, messageReceivedTime, caseId, productCode);
      }
    } else {
      caseId = caseCache.caseId;
      if ("CANCEL".equals(caseCache.lastActionInstruction) || "CANCEL(HELD)".equals(caseCache.lastActionInstruction)) {
        caseId = pauseRequest.getPayload().getFulfilmentRequest().getCaseId();
        eventManager.triggerEvent(caseId, CASE_ALREADY_CANCELLED);
      } else {
        sendPause(pauseRequest, messageReceivedTime, caseId, productCode);
      }
    }
  }

  private void sendPause(PauseOutcome pauseRequest, Instant messageReceivedTime, String caseId, String productCode) {
    String pauseRule;
    pauseRule = pauseRulesLookup.getLookup(pauseRequest.getPayload().getFulfilmentRequest().getFulfilmentCode());
    if (pauseRule == null) {
      eventManager.triggerEvent(caseId, "Could not find a rule for the fulfilment request and product code.",
          UNRECOGNISED_FULFILLMENT_CODE, "Product code", productCode);
    } else if (caseId != null) {
      FwmtActionInstruction pauseActionInstruction = buildPause(messageReceivedTime, caseId, pauseRule);
      messagePublisher.pausePublish(pauseActionInstruction);
      eventManager.triggerEvent(pauseRequest.getPayload().getFulfilmentRequest().getCaseId(), PAUSE_PROCESSED_AND_SENT);
    }
  }

  private FwmtActionInstruction buildPause(Instant messageReceivedTime, String caseId, String pauseRule) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
    Date date = Date.from(messageReceivedTime);
    String currentDate;
    currentDate = dateFormat.format(date);
    return FwmtActionInstruction.builder()
        .caseId(caseId)
        .actionInstruction(ActionInstructionType.PAUSE)
        .surveyName("CENSUS")
        .addressType("HH")
        .addressLevel("U")
        .pauseFrom(currentDate)
        .pauseCode(pauseRule)
        .build();
  }
}
