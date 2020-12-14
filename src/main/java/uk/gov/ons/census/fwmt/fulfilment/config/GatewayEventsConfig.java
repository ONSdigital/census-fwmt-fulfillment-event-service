package uk.gov.ons.census.fwmt.fulfilment.config;

import com.godaddy.logging.LoggingConfigs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.events.producer.GatewayLoggingEventProducer;
import uk.gov.ons.census.fwmt.events.producer.RabbitMQGatewayEventProducer;
import uk.gov.ons.census.fwmt.fulfilment.Application;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Function;

@Slf4j
@Configuration
public class GatewayEventsConfig {

  public static final String RECEIVED_FULFILMENT = "RECEIVED_FULFILMENT";

  public static final String ROUTING_FAILED = "ROUTING_FAILED";

  public static final String CONVERT_SPG_UNIT_UPDATE_TO_CREATE = "CONVERT_SPG_UNIT_UPDATE_TO_CREATE";

  private final boolean useJsonLogging;

  public GatewayEventsConfig(@Value("#{'${logging.profile}' == 'CLOUD'}") boolean useJsonLogging) {
    this.useJsonLogging = useJsonLogging;
  }

  @Value("${app.testing}")
  private boolean testing;

  // TODO - We need to move this out to the event library - it's used across all of the dependent services
  @Bean
  public GatewayEventManager gatewayEventManager(GatewayLoggingEventProducer gatewayLoggingEventProducer, RabbitMQGatewayEventProducer testProducer) {

    final GatewayEventManager gatewayEventManager;
    if (testing) {
      log.warn("\n\n \t IMPORTANT - Test Mode: ON        \n \t\t Service is initiated in test mode which, this should not occur in production \n\n");
      gatewayEventManager = new GatewayEventManager(Arrays.asList(gatewayLoggingEventProducer, testProducer));
    } else {
      log.warn("\n\n \t IMPORTANT - Test Mode: OFF   \n\n");
      gatewayEventManager = new GatewayEventManager(Arrays.asList(gatewayLoggingEventProducer));
    }

    gatewayEventManager.setSource(Application.APPLICATION_NAME);
    return gatewayEventManager;
  }

  @PostConstruct
  public void initJsonLogging() {
    HashMap<Class<?>, Function<Object, String>> customMappers = new HashMap<>();
    customMappers.put(LocalTime.class, Object::toString);
    customMappers.put(LocalDateTime.class, Object::toString);

    LoggingConfigs configs;

    if (useJsonLogging) {
      configs = LoggingConfigs.builder().customMapper(customMappers).build().useJson();
    } else {
      configs = LoggingConfigs.builder().customMapper(customMappers).build();
    }
    LoggingConfigs.setCurrent(configs);
  }
}
