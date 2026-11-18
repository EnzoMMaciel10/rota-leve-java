package br.com.fiap.rotaleve.controller;

import br.com.fiap.rotaleve.model.Notificacao;
import br.com.fiap.rotaleve.repository.NotificacaoRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;

@RestController @RequestMapping("/api/v1/notificacoes") @RequiredArgsConstructor
@Tag(name = "Notificações", description = "Alertas das rotas salvas do usuário autenticado")
@Transactional(readOnly = true)
public class NotificacaoController {
    private final NotificacaoRepository notificacoes;

    @GetMapping
    public Page<NotificacaoResponse> listar(@AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "criadaEm") Pageable pageable) {
        return notificacoes.findByUsuarioId(Long.valueOf(jwt.getSubject()), pageable).map(NotificacaoResponse::de);
    }

    @PatchMapping("/{id}/lida") @Transactional
    public NotificacaoResponse marcarLida(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        var notificacao = notificacoes.findById(id)
                .filter(n -> n.getUsuario().getId().equals(Long.valueOf(jwt.getSubject())))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificação não encontrada"));
        notificacao.setLida(true);
        return NotificacaoResponse.de(notificacao);
    }

    public record NotificacaoResponse(Long id, Long rotaId, String mensagem, boolean lida, Instant criadaEm) {
        static NotificacaoResponse de(Notificacao notificacao) {
            return new NotificacaoResponse(notificacao.getId(), notificacao.getRotaSalva().getId(),
                    notificacao.getMensagem(), notificacao.isLida(), notificacao.getCriadaEm());
        }
    }
}
