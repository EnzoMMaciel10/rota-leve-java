package br.com.fiap.rotaleve.controller;

import br.com.fiap.rotaleve.dto.CoreDtos.*;
import br.com.fiap.rotaleve.model.Tipos.StatusRelato;
import br.com.fiap.rotaleve.security.TokenService;
import br.com.fiap.rotaleve.service.CoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1") @RequiredArgsConstructor @Validated
@SecurityRequirement(name = "bearerAuth")
public class CoreController {
    private final CoreService service;
    private final TokenService tokens;
    @PostMapping("/auth/cadastro") @Operation(summary = "Cadastrar cidadão; papel de gestor não pode ser solicitado")
    public ResponseEntity<UsuarioResponse> cadastro(@Valid @RequestBody CadastroRequest dto) {
        UsuarioResponse usuario = service.cadastrar(dto); return ResponseEntity.created(URI.create("/api/v1/usuarios/me")).body(usuario);
    }
    @PostMapping("/auth/login") @Operation(summary = "Autenticar e obter JWT com validade configurada")
    public LoginResponse login(@Valid @RequestBody LoginRequest dto) { return tokens.login(dto); }
    @GetMapping("/usuarios/me")
    public UsuarioResponse usuario(@AuthenticationPrincipal Jwt jwt) { return service.obterUsuario(id(jwt)); }
    @PutMapping("/usuarios/me")
    public UsuarioResponse atualizarUsuario(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UsuarioRequest dto) { return service.atualizarUsuario(id(jwt), dto); }
    @DeleteMapping("/usuarios/me") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirUsuario(@AuthenticationPrincipal Jwt jwt) { service.excluirUsuario(id(jwt)); }
    @GetMapping("/perfis/me")
    public PerfilResponse perfil(@AuthenticationPrincipal Jwt jwt) { return service.obterPerfil(id(jwt)); }
    @PutMapping("/perfis/me") @Operation(summary = "Criar ou atualizar o próprio perfil de mobilidade")
    public PerfilResponse salvarPerfil(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PerfilRequest dto) { return service.salvarPerfil(id(jwt), dto); }
    @DeleteMapping("/perfis/me") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirPerfil(@AuthenticationPrincipal Jwt jwt) { service.excluirPerfil(id(jwt)); }
    @GetMapping("/pontos")
    public Page<PontoResponse> pontos(@PageableDefault(size = 20, sort = "id") Pageable pageable) { return service.listarPontos(pageable); }
    @GetMapping("/pontos/{id}")
    public PontoResponse ponto(@PathVariable @Positive Long id) { return service.obterPonto(id); }
    @PostMapping("/pontos") @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<PontoResponse> criarPonto(@Valid @RequestBody PontoRequest dto) {
        PontoResponse ponto = service.criarPonto(dto); return ResponseEntity.created(URI.create("/api/v1/pontos/" + ponto.id())).body(ponto);
    }
    @PutMapping("/pontos/{id}") @PreAuthorize("hasRole('GESTOR')")
    public PontoResponse atualizarPonto(@PathVariable @Positive Long id, @Valid @RequestBody PontoRequest dto) { return service.atualizarPonto(id, dto); }
    @DeleteMapping("/pontos/{id}") @PreAuthorize("hasRole('GESTOR')") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirPonto(@PathVariable @Positive Long id) { service.excluirPonto(id); }
    @GetMapping("/relatos")
    public Page<RelatoResponse> relatos(@RequestParam(required = false) StatusRelato status,
        @PageableDefault(size = 20, sort = "id") Pageable pageable) { return service.listarRelatos(status, pageable); }
    @GetMapping("/relatos/{id}")
    public RelatoResponse relato(@PathVariable @Positive Long id) { return service.obterRelato(id); }
    @PostMapping("/relatos")
    public ResponseEntity<RelatoResponse> criarRelato(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RelatoRequest dto) {
        RelatoResponse relato = service.criarRelato(id(jwt), dto); return ResponseEntity.created(URI.create("/api/v1/relatos/" + relato.id())).body(relato);
    }
    @PutMapping("/relatos/{id}")
    public RelatoResponse atualizarRelato(@AuthenticationPrincipal Jwt jwt, @PathVariable @Positive Long id,
        @Valid @RequestBody RelatoRequest dto) { return service.atualizarRelato(id(jwt), id, dto); }
    @DeleteMapping("/relatos/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirRelato(@AuthenticationPrincipal Jwt jwt, @PathVariable @Positive Long id) { service.excluirRelato(id(jwt), id); }
    @PostMapping("/relatos/{id}/confirmacoes") @Operation(summary = "Confirmar relato uma única vez por cidadão")
    public ConfirmacaoResponse confirmar(@AuthenticationPrincipal Jwt jwt, @PathVariable @Positive Long id) { return service.confirmar(id(jwt), id); }
    @PostMapping("/relatos/{id}/resolver") @PreAuthorize("hasRole('GESTOR')")
    public RelatoResponse resolver(@PathVariable @Positive Long id) { return service.resolver(id); }
    private Long id(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
}
