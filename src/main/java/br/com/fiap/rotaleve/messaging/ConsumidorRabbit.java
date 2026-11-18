package br.com.fiap.rotaleve.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
@ConditionalOnProperty(name = "rotaleve.rabbit.enabled", havingValue = "true")
public class ConsumidorRabbit {
    private final NotificacaoService notificacoes;

    @RabbitListener(queues = RabbitConfig.FILA)
    public void receber(EventoPonto evento) {
        notificacoes.consumir(evento);
    }
}
