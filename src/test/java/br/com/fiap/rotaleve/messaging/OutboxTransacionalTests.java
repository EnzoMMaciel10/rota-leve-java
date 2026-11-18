package br.com.fiap.rotaleve.messaging;

import br.com.fiap.rotaleve.service.PontoAlteradoEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.transaction.TestTransaction;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({RegistroOutbox.class, OutboxTransacionalTests.Tempo.class})
class OutboxTransacionalTests {
    @Autowired ApplicationEventPublisher eventos;
    @Autowired EventoOutboxRepository outbox;

    @Test
    void eventoEhPersistidoNoMesmoCommitDaAlteracao() {
        eventos.publishEvent(new PontoAlteradoEvent(9876L, "teste-commit-outbox"));
        assertThat(outbox.findAll()).noneMatch(e -> e.getMensagem().equals("teste-commit-outbox"));

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        assertThat(outbox.findAll()).anySatisfy(e -> {
            assertThat(e.getMensagem()).isEqualTo("teste-commit-outbox");
            assertThat(e.isEnviado()).isFalse();
            assertThat(e.getPontoId()).isEqualTo(9876L);
        });
    }

    @Test
    void rollbackDaAlteracaoNaoDeixaEventoFantasma() {
        eventos.publishEvent(new PontoAlteradoEvent(9877L, "teste-rollback-outbox"));
        TestTransaction.flagForRollback();
        TestTransaction.end();
        TestTransaction.start();

        assertThat(outbox.findAll()).noneMatch(e -> e.getMensagem().equals("teste-rollback-outbox"));
    }

    @TestConfiguration
    static class Tempo {
        @Bean @Primary
        Clock clockOutboxTeste() {
            return Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);
        }
    }
}
