package br.com.orders.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    @Value("${app.rabbitmq.queues.incoming}")
    private String incomingQueueName;
    
    @Value("${app.rabbitmq.queues.dlq}")
    private String dlqName;
    
    @Value("${app.rabbitmq.exchanges.incoming}")
    private String incomingExchangeName;
    
    @Bean
    public TopicExchange incomingExchange() {
        return new TopicExchange(incomingExchangeName, true, false);
    }
    
    @Bean
    public Queue incomingQueue() {
        return QueueBuilder.durable(incomingQueueName)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", dlqName)
                .withArgument("x-message-ttl", 300000) // 5 minutes
                .build();
    }
    
    @Bean
    public Queue dlq() {
        return QueueBuilder.durable(dlqName).build();
    }
    
    @Bean
    public Binding incomingBinding() {
        return BindingBuilder
                .bind(incomingQueue())
                .to(incomingExchange())
                .with("order.created");
    }
    
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
    
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(1);
        return factory;
    }
}
