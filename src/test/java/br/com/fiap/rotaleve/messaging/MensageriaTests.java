package br.com.fiap.rotaleve.messaging;

import br.com.fiap.rotaleve.model.Notificacao;
import br.com.fiap.rotaleve.model.RotaSalva;
import br.com.fiap.rotaleve.model.Usuario;
import br.com.fiap.rotaleve.repository.NotificacaoRepository;
import br.com.fiap.rotaleve.repository.RotaSalvaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MensageriaTests {
    private static final Instant AGORA = Instant.parse("2026-11-16T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AGORA, ZoneOffset.UTC);
    private static final EventoPonto EVENTO = new EventoPonto("evento-1", 10L, "Elevador indisponível.", AGORA);
    @Mock RotaSalvaRepository rotas;
    @Mock NotificacaoRepository notificacoes;
    @Mock EventoOutboxRepository outbox;
    @Mock EntregaEvento entrega;
    @Mock RabbitTemplate rabbit;

    @Test
    void eventoReentregueNaoDuplicaNotificacaoDaMesmaRota() {
        var usuario = new Usuario();
        usuario.setId(4L);
        var rota = new RotaSalva();
        rota.setId(7L);
        rota.setUsuario(usuario);
        when(rotas.findAllByPontosCriticosId(10L)).thenReturn(List.of(rota));
        when(notificacoes.existsByEventoIdAndRotaSalvaId("evento-1", 7L)).thenReturn(false, true);
        var service = new NotificacaoService(rotas, notificacoes, CLOCK);

        service.consumir(EVENTO);
        service.consumir(EVENTO);

        var captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacoes, times(1)).save(captor.capture());
        assertThat(captor.getValue().getUsuario().getId()).isEqualTo(4L);
        assertThat(captor.getValue().getEventoId()).isEqualTo("evento-1");
        assertThat(captor.getValue().isLida()).isFalse();
        assertThat(captor.getValue().getCriadaEm()).isEqualTo(AGORA);
    }

    @Test
    void pontoForaDasRotasSalvasNaoGeraAlerta() {
        when(rotas.findAllByPontosCriticosId(10L)).thenReturn(List.of());
        new NotificacaoService(rotas, notificacoes, CLOCK).consumir(EVENTO);
        verifyNoInteractions(notificacoes);
    }

    @Test
    void somenteMarcaEnviadoDepoisDeTransporteConfirmar() throws Exception {
        var registro = registro();
        when(outbox.bloquear("evento-1")).thenReturn(Optional.of(registro));
        new ProcessadorOutbox(outbox, entrega, CLOCK).processar("evento-1");
        verify(entrega).entregar(EVENTO);
        assertThat(registro.isEnviado()).isTrue();
        assertThat(registro.getEnviadoEm()).isEqualTo(AGORA);
        assertThat(registro.getTentativas()).isEqualTo(1);
    }

    @Test
    void falhaMantemPendenteERetryFicaParaDepois() throws Exception {
        var registro = registro();
        when(outbox.bloquear("evento-1")).thenReturn(Optional.of(registro));
        doThrow(new AmqpException("Indisponível")).when(entrega).entregar(any());
        var processador = new ProcessadorOutbox(outbox, entrega, CLOCK);

        assertThatThrownBy(() -> processador.processar("evento-1")).isInstanceOf(AmqpException.class);
        assertThat(registro.isEnviado()).isFalse();
        processador.registrarFalha("evento-1", "AmqpException");
        assertThat(registro.getTentativas()).isEqualTo(1);
        assertThat(registro.getProximaTentativaEm()).isEqualTo(AGORA.plusSeconds(5));
        assertThat(registro.getUltimaFalha()).isEqualTo("AmqpException");
    }

    @Test
    void registroJaEnviadoNaoVoltaAoTransporte() throws Exception {
        var registro = registro();
        registro.setEnviado(true);
        when(outbox.bloquear("evento-1")).thenReturn(Optional.of(registro));
        new ProcessadorOutbox(outbox, entrega, CLOCK).processar("evento-1");
        verifyNoInteractions(entrega);
    }

    @Test
    void publisherExigeAckEPedeMensagemPersistente() throws Exception {
        doAnswer(invocacao -> {
            MessagePostProcessor postProcessor = invocacao.getArgument(3);
            var mensagem = postProcessor.postProcessMessage(new Message(new byte[0]));
            assertThat(mensagem.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
            assertThat(mensagem.getMessageProperties().getMessageId()).isEqualTo("evento-1");
            CorrelationData correlacao = invocacao.getArgument(4);
            correlacao.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY), eq(EVENTO),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        new EntregaRabbit(rabbit).entregar(EVENTO);
        verify(rabbit).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY), eq(EVENTO),
                any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test
    void nackDoBrokerMantemEventoParaReenvio() {
        doAnswer(invocacao -> {
            CorrelationData correlacao = invocacao.getArgument(4);
            correlacao.getFuture().complete(new CorrelationData.Confirm(false, "falha"));
            return null;
        }).when(rabbit).convertAndSend(anyString(), anyString(), any(),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        assertThatThrownBy(() -> new EntregaRabbit(rabbit).entregar(EVENTO)).isInstanceOf(AmqpException.class);
    }

    @Test
    void eventoIncompletoEhRejeitadoAntesDeProcessar() {
        assertThatThrownBy(() -> new EventoPonto("", 10L, "Alerta", AGORA)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EventoPonto("id", null, "Alerta", AGORA)).isInstanceOf(IllegalArgumentException.class);
    }

    private EventoOutbox registro() {
        var registro = new EventoOutbox();
        registro.setId(EVENTO.eventId());
        registro.setPontoId(EVENTO.pontoId());
        registro.setMensagem(EVENTO.mensagem());
        registro.setCriadoEm(EVENTO.ocorridoEm());
        registro.setProximaTentativaEm(AGORA);
        return registro;
    }
}
