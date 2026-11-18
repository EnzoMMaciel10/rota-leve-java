package br.com.fiap.rotaleve.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "rotaleve.rabbit.enabled", havingValue = "true")
public class RabbitConfig {
    public static final String EXCHANGE = "rotaleve.pontos";
    public static final String FILA = "rotaleve.alertas";
    public static final String DLX = "rotaleve.dlx";
    public static final String DLQ = "rotaleve.alertas.dlq";
    public static final String ROUTING_KEY = "ponto.alterado";

    @Bean
    public Declarables topologia() {
        var exchange = new TopicExchange(EXCHANGE, true, false);
        var dlx = new DirectExchange(DLX, true, false);
        var fila = QueueBuilder.durable(FILA).deadLetterExchange(DLX).deadLetterRoutingKey(DLQ).build();
        var dlq = QueueBuilder.durable(DLQ).build();
        return new Declarables(exchange, dlx, fila, dlq,
                BindingBuilder.bind(fila).to(exchange).with(ROUTING_KEY),
                BindingBuilder.bind(dlq).to(dlx).with(DLQ));
    }

    @Bean
    public Jackson2JsonMessageConverter conversorMensagens(ObjectMapper mapper) {
        return new Jackson2JsonMessageConverter(mapper, "br.com.fiap.rotaleve.messaging");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter conversor) {
        if (connectionFactory instanceof CachingConnectionFactory caching) {
            caching.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
            caching.setPublisherReturns(true);
        }
        var template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(conversor);
        template.setMandatory(true);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter conversor) {
        var factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(conversor);
        factory.setConcurrentConsumers(1);
        factory.setPrefetchCount(20);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless().maxAttempts(3)
                .backOffOptions(500, 2, 2000).recoverer(new RejectAndDontRequeueRecoverer()).build());
        return factory;
    }
}
