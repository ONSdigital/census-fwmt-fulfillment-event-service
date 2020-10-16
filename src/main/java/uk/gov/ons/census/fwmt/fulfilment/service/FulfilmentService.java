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
import uk.gov.ons.census.fwmt.fulfilment.lookup.PauseRulesLookup;
import uk.gov.ons.census.fwmt.fulfilment.rabbit.MessagePublisher;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;

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

  public void processPauseCase(PauseOutcome pauseRequest) {
    FwmtActionInstruction pauseActionInstruction = new FwmtActionInstruction();
    String caseId = "";
    String pauseRule;
    String productCode = pauseRequest.getPayload().getFulfilmentRequest().getFulfilmentCode();

    final String caseCache = cacheService.getByIdAndTypeAndExists(pauseRequest.getPayload().getFulfilmentRequest()
        .getCaseId(), 10, true);
    final String indCache = cacheService.getByIndividualCaseIdAndTypeAndExists(pauseRequest.getPayload().getFulfilmentRequest()
        .getIndividualCaseId(), 10, true);

    if (caseCache == null && indCache == null){
      caseId = pauseRequest.getPayload().getFulfilmentRequest().getCaseId();
      eventManager.triggerErrorEvent(this.getClass(), "Could not find an existing record",
          caseId, "ROUTING_FAILED");
      throw new AmqpRejectAndDontRequeueException(null, true, null);
    } else if (caseCache == null) {
      caseId = indCache;
    } else {
      caseId = caseCache;
    }

    pauseRule = pauseRulesLookup.getLookup(pauseRequest.getPayload().getFulfilmentRequest().getFulfilmentCode());

    if (pauseRule == null) {
      eventManager.triggerErrorEvent(this.getClass(), "Could not find a rule for the fulfilment request and product code.",
          String.valueOf(caseId), "Product code: " + productCode);
      throw new AmqpRejectAndDontRequeueException(null, true, null);
    } else {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH);
      String date = LocalDateTime.now().toString();
      Date currentDate;
      try {
        currentDate = dateFormat.parse(date);
        pauseActionInstruction.setPauseFrom(currentDate);
      } catch (ParseException e){
        String error = e.toString();
        System.out.println(error);
      }


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
