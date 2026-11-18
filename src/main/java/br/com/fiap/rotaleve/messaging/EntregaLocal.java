package br.com.fiap.rotaleve.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Demonstração local assíncrona pelo agendador; este adaptador não utiliza RabbitMQ. */
@Component @RequiredArgsConstructor
@ConditionalOnProperty(name = "rotaleve.rabbit.enabled", havingValue = "false", matchIfMissing = true)
public class EntregaLocal implements EntregaEvento {
    private final NotificacaoService notificacoes;

    @Override
    public void entregar(EventoPonto evento) {
        notificacoes.consumir(evento);
    }
}
