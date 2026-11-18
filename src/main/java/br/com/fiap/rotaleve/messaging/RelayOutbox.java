package br.com.fiap.rotaleve.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Clock;

@Component @RequiredArgsConstructor @EnableScheduling @Slf4j
public class RelayOutbox {
    private final EventoOutboxRepository outbox;
    private final ProcessadorOutbox processador;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${rotaleve.outbox.intervalo-ms:5000}")
    public void processarPendentes() {
        var pendentes = outbox.findTop50ByEnviadoFalseAndProximaTentativaEmLessThanEqualOrderByCriadoEmAsc(clock.instant());
        for (var registro : pendentes) {
            try {
                processador.processar(registro.getId());
            } catch (Exception falha) {
                // Não registrar corpo do evento nem mensagens de conexão com possíveis segredos.
                log.warn("Evento {} permanece pendente ({})", registro.getId(), falha.getClass().getSimpleName());
                processador.registrarFalha(registro.getId(), falha.getClass().getSimpleName());
            }
        }
    }
}
