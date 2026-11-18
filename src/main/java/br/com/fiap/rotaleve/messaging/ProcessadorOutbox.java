package br.com.fiap.rotaleve.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;

@Service @RequiredArgsConstructor
public class ProcessadorOutbox {
    private final EventoOutboxRepository outbox;
    private final EntregaEvento entrega;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void processar(String id) throws Exception {
        var registro = outbox.bloquear(id).orElse(null);
        if (registro == null || registro.isEnviado()
                || registro.getProximaTentativaEm().isAfter(clock.instant())) {
            return;
        }
        entrega.entregar(registro.evento());
        registro.setEnviado(true);
        registro.setEnviadoEm(clock.instant());
        registro.setTentativas(registro.getTentativas() + 1);
        registro.setUltimaFalha(null);
    }

    /** Nova transação: uma falha no transporte não apaga a tentativa nem o evento. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFalha(String id, String tipoFalha) {
        outbox.bloquear(id).filter(registro -> !registro.isEnviado()).ifPresent(registro -> {
            int tentativas = registro.getTentativas() + 1;
            registro.setTentativas(tentativas);
            registro.setUltimaFalha(tipoFalha.substring(0, Math.min(tipoFalha.length(), 120)));
            long atraso = Math.min(300L, 5L * (1L << Math.min(tentativas - 1, 6)));
            registro.setProximaTentativaEm(clock.instant().plusSeconds(atraso));
        });
    }
}
