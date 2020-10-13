package uk.gov.ons.census.fwmt.fulfilment.config;

import com.rabbitmq.client.Channel;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.support.RetryTemplate;
import uk.gov.ons.census.fwmt.common.retry.DefaultListenerSupport;
import uk.gov.ons.census.fwmt.common.retry.GatewayMessageRecover;
import uk.gov.ons.census.fwmt.common.retry.GatewayRetryPolicy;
import uk.gov.ons.census.fwmt.fulfilment.rabbit.FulfilmentEventReceiver;

import java.io.IOException;

@Configuration
public class RabbitMqConfig {
  public final String exchange;
  public final String routingKey;
  private final String username;
  private final String password;
  private final String hostname;
  private final int port;
  private final String virtualHost;
  private final int initialInterval;
  private final double multiplier;
  private final int maxInterval;
  private final int prefetchCount;
  public RabbitMqConfig(
      @Value("${rabbitmq.username}") String username,
      @Value("${rabbitmq.password}") String password,
      @Value("${rabbitmq.hostname}") String hostname,
      @Value("${rabbitmq.port}") int port,
      @Value("${rabbitmq.virtualHost}") String virtualHost,
      @Value("${rabbitmq.initialInterval}") int initialInterval,
      @Value("${rabbitmq.multiplier}") double multiplier,
      @Value("${rabbitmq.maxInterval}") int maxInterval,
      @Value("${rabbitmq.prefetchCount}") int prefetchCount,
      @Value("${rabbitmq.exchange.routingKey}") String routingKey,
      @Value("${rabbitmq.exchange.exchange}") String exchange) {
    this.username = username;
    this.password = password;
    this.hostname = hostname;
    this.port = port;
    this.virtualHost = virtualHost;
    this.initialInterval = initialInterval;
    this.multiplier = multiplier;
    this.maxInterval = maxInterval;
    this.exchange = exchange;
    this.routingKey = routingKey;
    this.prefetchCount = prefetchCount;
  }
  @Bean
  public ConnectionFactory connectionFactory() {
    CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(hostname, port);
    cachingConnectionFactory.setVirtualHost(virtualHost);
    cachingConnectionFactory.setPassword(password);
    cachingConnectionFactory.setUsername(username);
    return cachingConnectionFactory;
  }
  @Bean
  public AmqpAdmin amqpAdmin() {
    return new RabbitAdmin(connectionFactory());
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

    GatewayRetryPolicy gatewayRetryPolicy = new GatewayRetryPolicy();
    retryTemplate.setRetryPolicy(gatewayRetryPolicy);

    retryTemplate.registerListener(new DefaultListenerSupport());

    return retryTemplate;
  }

  @Bean
  public MessageListenerAdapter listenerAdapter(FulfilmentEventReceiver receiver) {
    return new MessageListenerAdapter(receiver, "receiveMessage");
  }

  //Message Listener
  @Bean
  public SimpleMessageListenerContainer gatewayActionsMessageListener(
      @Qualifier("connectionFactory") ConnectionFactory connectionFactory,
      @Qualifier("listenerAdapter") MessageListenerAdapter messageListenerAdapter,
      @Qualifier("interceptor") RetryOperationsInterceptor retryOperationsInterceptor) throws IOException {
    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
    Channel channel = connectionFactory.createConnection().createChannel(false);
    channel.queueDeclare(routingKey, true, false, false, null);
    channel.queueBind(routingKey, exchange, "event.fulfilment.request");
    Advice[] adviceChain = {retryOperationsInterceptor};
    messageListenerAdapter.setMessageConverter(new Jackson2JsonMessageConverter());
    container.setAdviceChain(adviceChain);
    container.setConnectionFactory(connectionFactory);
    container.setQueueNames(routingKey);
    container.setMessageListener(messageListenerAdapter);
    return container;
  }
}