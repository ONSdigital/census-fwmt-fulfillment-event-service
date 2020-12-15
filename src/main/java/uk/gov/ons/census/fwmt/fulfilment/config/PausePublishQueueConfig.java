package uk.gov.ons.census.fwmt.fulfilment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtPauseActionInstruction;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class PausePublishQueueConfig {

  @Bean(name = "republishRabbitTemplate")
  public RabbitTemplate rabbitTemplate(@Qualifier("RP_MC") MessageConverter mc,
      @Qualifier("rmConnectionFactory") ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(mc);
    return template;
  }

  @Bean(name = "republishJsonMessageConverter")
  @Qualifier("RP_MC")
  public MessageConverter jsonMessageConverter(@Qualifier("RP_CM") DefaultClassMapper cm) {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    Jackson2JsonMessageConverter jsonMessageConverter = new Jackson2JsonMessageConverter(objectMapper);
    jsonMessageConverter.setClassMapper(cm);
    return jsonMessageConverter;
  }

  @Bean(name = "republishClassMapper")
  @Qualifier("RP_CM")
  public DefaultClassMapper classMapper() {
    DefaultClassMapper classMapper = new DefaultClassMapper();
    Map<String, Class<?>> idClassMapping = new HashMap<>();
    idClassMapping.put("uk.gov.ons.census.fwmtadapter.model.dto.fwmt.FwmtPauseActionInstruction", FwmtPauseActionInstruction.class);
    idClassMapping.put("uk.gov.ons.census.fwmt.common.rm.dto.FwmtPauseActionInstruction", FwmtPauseActionInstruction.class);
    classMapper.setIdClassMapping(idClassMapping);
    classMapper.setTrustedPackages("*");
    return classMapper;
  }
}
