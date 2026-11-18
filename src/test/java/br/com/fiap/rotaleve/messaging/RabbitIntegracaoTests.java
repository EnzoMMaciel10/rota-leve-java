package br.com.fiap.rotaleve.messaging;

import br.com.fiap.rotaleve.model.*;
import br.com.fiap.rotaleve.model.Tipos.*;
import br.com.fiap.rotaleve.repository.*;
import br.com.fiap.rotaleve.service.PontoAlteradoEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Teste opt-in: exige RabbitMQ real exclusivo para testes, sem outra instância da aplicação
 * consumindo as mesmas filas. Não é substituído por mocks nem executado na suíte offline.
 * O broker e o virtual host podem ser configurados por SPRING_RABBITMQ_*.
 */
@SpringBootTest(properties = {
        "rotaleve.rabbit.enabled=true", "rotaleve.ai.enabled=false", "spring.ai.model.chat=none",
        "spring.datasource.url=jdbc:h2:mem:rabbitintegration;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.listener.simple.auto-startup=true"
})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RABBIT_INTEGRATION_TEST", matches = "(?i)true")
class RabbitIntegracaoTests {
    @Autowired TransactionTemplate transaction;
    @Autowired UsuarioRepository usuarios;
    @Autowired PontoCriticoRepository pontos;
    @Autowired RotaSalvaRepository rotas;
    @Autowired EventoOutboxRepository outbox;
    @Autowired NotificacaoRepository notificacoes;
    @Autowired ApplicationEventPublisher eventos;
    @Autowired RelayOutbox relay;
    @Autowired AmqpAdmin admin;

    @Test
    void commitPublicaNoBrokerEConsumidorPersisteAlerta() {
        String mensagem = "integração-rabbit-" + UUID.randomUUID();
        Long rotaId = transaction.execute(status -> {
            var usuario = new Usuario();
            usuario.setNome("Usuário teste Rabbit");
            usuario.setEmail(UUID.randomUUID() + "@teste.rotaleve.local");
            usuario.setSenhaHash("hash-ficticio-apenas-no-banco-em-memoria-do-teste");
            usuario.setPapel(Papel.CIDADAO);
            usuarios.save(usuario);
            var ponto = new PontoCritico();
            ponto.setNome("Elevador teste Rabbit");
            ponto.setTipo(TipoPonto.ELEVADOR);
            ponto.setEstado(EstadoPonto.PROBLEMA);
            ponto.setUltimaVerificacao(Instant.now());
            pontos.save(ponto);
            var rota = new RotaSalva();
            rota.setUsuario(usuario);
            rota.setNome("Rota teste Rabbit");
            rota.setOrigem("Origem sintética");
            rota.setDestino("Destino sintético");
            rota.setSnapshotJson("{}");
            rota.setCriadaEm(Instant.now());
            rota.getPontosCriticos().add(ponto);
            rotas.save(rota);
            eventos.publishEvent(new PontoAlteradoEvent(ponto.getId(), mensagem));
            return rota.getId();
        });
        var registro = outbox.findAll().stream().filter(e -> e.getMensagem().equals(mensagem)).findFirst().orElseThrow();

        relay.processarPendentes();

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(notificacoes.existsByEventoIdAndRotaSalvaId(registro.getId(), rotaId)).isTrue());
        assertThat(outbox.findById(registro.getId()).orElseThrow().isEnviado()).isTrue();
        assertThat(admin.getQueueProperties(RabbitConfig.FILA)).isNotNull();
        assertThat(admin.getQueueProperties(RabbitConfig.DLQ)).isNotNull();
    }
}
