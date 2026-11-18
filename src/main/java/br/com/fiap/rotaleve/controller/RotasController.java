package br.com.fiap.rotaleve.controller;

import br.com.fiap.rotaleve.routes.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/rotas") @RequiredArgsConstructor
public class RotasController {
    private final RotasService rotas;
    private final ExplicacaoService explicacoes;
    @GetMapping("/cenarios") public Map<String, String> cenarios() {
        return Map.of("origem", "Praça da Sé", "destino", "Hospital das Clínicas", "aviso", "Cenário sintético suportado pelo provedor demo");
    }
    @PostMapping("/busca") public RotasDtos.BuscaResponse buscar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RotasDtos.BuscaRequest request) {
        return rotas.buscar(id(jwt), request);
    }
    @PostMapping("/salvas") public ResponseEntity<RotasDtos.SalvaResponse> salvar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RotasDtos.SalvarRequest request) {
        var resposta = rotas.salvar(id(jwt), request);
        return ResponseEntity.created(URI.create("/api/v1/rotas/salvas/" + resposta.id())).body(resposta);
    }
    @GetMapping("/salvas") public Page<RotasDtos.SalvaResponse> listar(@AuthenticationPrincipal Jwt jwt, Pageable pageable) { return rotas.listar(id(jwt), pageable); }
    @GetMapping("/salvas/{rotaId}") public RotasDtos.SalvaResponse detalhe(@AuthenticationPrincipal Jwt jwt, @PathVariable long rotaId) { return rotas.detalhar(id(jwt), rotaId); }
    @PutMapping("/salvas/{rotaId}") public RotasDtos.SalvaResponse renomear(@AuthenticationPrincipal Jwt jwt, @PathVariable long rotaId, @Valid @RequestBody RotasDtos.NomeRequest request) {
        return rotas.renomear(id(jwt), rotaId, request);
    }
    @DeleteMapping("/salvas/{rotaId}") public ResponseEntity<Void> excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable long rotaId) {
        rotas.excluir(id(jwt), rotaId); return ResponseEntity.noContent().build();
    }
    @PostMapping("/salvas/{rotaId}/explicacao") public RotasDtos.ExplicacaoResponse explicar(@AuthenticationPrincipal Jwt jwt, @PathVariable long rotaId) {
        return explicacoes.explicar(rotas.detalhar(id(jwt), rotaId));
    }
    private long id(Jwt jwt) { return Long.parseLong(jwt.getSubject()); }
}
