package uk.gov.ons.census.fwmt.fulfilment.config;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.support.RetryTemplate;
import uk.gov.ons.census.fwmt.common.retry.DefaultListenerSupport;
import uk.gov.ons.census.fwmt.common.retry.GatewayMessageRecover;
import uk.gov.ons.census.fwmt.common.retry.GatewayRetryPolicy;

@Configuration
public class RabbitMqConfig {
  private final String username;
  private final String password;
  private final String hostname;
  private final int port;
  private final String virtualHost;
  private final int initialInterval;
  private final double multiplier;
  private final int maxInterval;

  public RabbitMqConfig(
      @Value("${app.rabbitmq.rm.username}") String username,
      @Value("${app.rabbitmq.rm.password}") String password,
      @Value("${app.rabbitmq.rm.host}") String hostname,
      @Value("${app.rabbitmq.rm.port}") int port,
      @Value("${app.rabbitmq.rm.virtualHost}") String virtualHost,
      @Value("${app.rabbitmq.rm.initialInterval}") int initialInterval,
      @Value("${app.rabbitmq.rm.multiplier}") double multiplier,
      @Value("${app.rabbitmq.rm.maxInterval}") int maxInterval) {
    this.username = username;
    this.password = password;
    this.hostname = hostname;
    this.port = port;
    this.virtualHost = virtualHost;
    this.initialInterval = initialInterval;
    this.multiplier = multiplier;
    this.maxInterval = maxInterval;
  }

  @Bean("rmConnectionFactory")
  public ConnectionFactory connectionFactory() {
    CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(hostname, port);
    cachingConnectionFactory.setVirtualHost(virtualHost);
    cachingConnectionFactory.setPassword(password);
    cachingConnectionFactory.setUsername(username);
    return cachingConnectionFactory;
  }

  @Bean
  public RetryOperationsInterceptor interceptor() {
    RetryOperationsInterceptor interceptor = new RetryOperationsInterceptor();
    interceptor.setRecoverer(new GatewayMessageRecover());
    interceptor.setRetryOperations(retryTemplate());
    return interceptor;
  }

  @Bean
  public RetryTemplate retryTemplate() {
    RetryTemplate retryTemplate = new RetryTemplate();
    ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
    backOffPolicy.setInitialInterval(initialInterval);
    backOffPolicy.setMultiplier(multiplier);
    backOffPolicy.setMaxInterval(maxInterval);
    retryTemplate.setBackOffPolicy(backOffPolicy);
    GatewayRetryPolicy gatewayRetryPolicy = new GatewayRetryPolicy(1);
    retryTemplate.setRetryPolicy(gatewayRetryPolicy);
    retryTemplate.registerListener(new DefaultListenerSupport());
    return retryTemplate;
  }

  @Bean
  public Jackson2JsonMessageConverter convertJsonMessage() {
    return jsonMessageConverter(Object.class);
  }

  public Jackson2JsonMessageConverter jsonMessageConverter(Class<?> defaultType) {
    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
    DefaultClassMapper mapper = new DefaultClassMapper();
    mapper.setDefaultType(defaultType);
    mapper.setTrustedPackages("*");
    DefaultClassMapper mapperTest =
        new DefaultClassMapper() {
          @Override
          public Class<?> toClass(MessageProperties properties) {
            return defaultType;
          }
        };
    converter.setClassMapper(mapperTest);
    return converter;
  }

  @Bean
  public SimpleRabbitListenerContainerFactory fulfilmentQueueListener(
      @Qualifier("rmConnectionFactory") ConnectionFactory connectionFactory, RetryOperationsInterceptor interceptor,
      @Qualifier("convertJsonMessage") MessageConverter messageConverter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(messageConverter);
    Advice[] adviceChain = {interceptor};
    factory.setAdviceChain(adviceChain);
    return factory;
  }
}