package br.com.fiap.rotaleve.messaging;

import br.com.fiap.rotaleve.service.PontoAlteradoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.time.Clock;
import java.util.UUID;

@Component @RequiredArgsConstructor
public class RegistroOutbox {
    private final EventoOutboxRepository outbox;
    private final Clock clock;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void registrar(PontoAlteradoEvent alteracao) {
        var registro = new EventoOutbox();
        registro.setId(UUID.randomUUID().toString());
        registro.setPontoId(alteracao.pontoId());
        registro.setMensagem(alteracao.mensagem());
        registro.setCriadoEm(clock.instant());
        registro.setProximaTentativaEm(clock.instant());
        outbox.save(registro);
    }
}
