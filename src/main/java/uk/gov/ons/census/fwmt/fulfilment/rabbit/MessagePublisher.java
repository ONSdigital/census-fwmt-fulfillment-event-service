package uk.gov.ons.census.fwmt.fulfilment.rabbit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtActionInstruction;

@Service
public class MessagePublisher {

  @Autowired
  @Qualifier("republishRabbitTemplate")
  private RabbitTemplate rabbitTemplate;

  public void pausePublish(FwmtActionInstruction pauseActionInstruction) {
    rabbitTemplate.convertAndSend("RM.Field", pauseActionInstruction);
  }
}
