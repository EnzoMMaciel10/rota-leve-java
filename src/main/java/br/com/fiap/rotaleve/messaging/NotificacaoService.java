package br.com.fiap.rotaleve.messaging;

import br.com.fiap.rotaleve.model.Notificacao;
import br.com.fiap.rotaleve.repository.NotificacaoRepository;
import br.com.fiap.rotaleve.repository.RotaSalvaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;

@Service @RequiredArgsConstructor
public class NotificacaoService {
    private final RotaSalvaRepository rotas;
    private final NotificacaoRepository notificacoes;
    private final Clock clock;

    /** A entrega é pelo menos uma vez; a chave evento/rota torna o efeito idempotente. */
    @Transactional
    public void consumir(EventoPonto evento) {
        for (var rota : rotas.findAllByPontosCriticosId(evento.pontoId())) {
            if (notificacoes.existsByEventoIdAndRotaSalvaId(evento.eventId(), rota.getId())) {
                continue;
            }
            var notificacao = new Notificacao();
            notificacao.setUsuario(rota.getUsuario());
            notificacao.setRotaSalva(rota);
            notificacao.setEventoId(evento.eventId());
            notificacao.setMensagem(evento.mensagem() + " Consulte as alternativas para sua rota salva.");
            notificacao.setCriadaEm(clock.instant());
            notificacao.setLida(false);
            notificacoes.save(notificacao);
        }
    }
}
