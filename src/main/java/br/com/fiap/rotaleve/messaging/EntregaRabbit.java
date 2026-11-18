package br.com.fiap.rotaleve.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component @RequiredArgsConstructor
@ConditionalOnProperty(name = "rotaleve.rabbit.enabled", havingValue = "true")
public class EntregaRabbit implements EntregaEvento {
    private final RabbitTemplate rabbit;

    @Override
    public void entregar(EventoPonto evento) throws Exception {
        var correlacao = new CorrelationData(evento.eventId());
        rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, evento, mensagem -> {
            mensagem.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            mensagem.getMessageProperties().setMessageId(evento.eventId());
            return mensagem;
        }, correlacao);
        var confirmacao = correlacao.getFuture().get(5, TimeUnit.SECONDS);
        if (!confirmacao.isAck() || correlacao.getReturned() != null) {
            throw new AmqpException("Broker não confirmou o encaminhamento para uma fila");
        }
    }
}
