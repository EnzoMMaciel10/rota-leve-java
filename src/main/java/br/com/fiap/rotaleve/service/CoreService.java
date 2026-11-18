package br.com.fiap.rotaleve.service;

import br.com.fiap.rotaleve.dto.CoreDtos.*;
import br.com.fiap.rotaleve.exception.RegraNegocioException;
import br.com.fiap.rotaleve.model.*;
import br.com.fiap.rotaleve.model.Tipos.*;
import br.com.fiap.rotaleve.repository.*;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras de titularidade, confirmação e validade dos relatos ficam nesta camada. */
@Service @RequiredArgsConstructor @Transactional
public class CoreService {
    private static final Duration VALIDADE = Duration.ofHours(48);
    private static final Duration VALIDADE_VERIFICACAO = Duration.ofHours(24);
    private final UsuarioRepository usuarios;
    private final PerfilMobilidadeRepository perfis;
    private final PontoCriticoRepository pontos;
    private final RelatoRepository relatos;
    private final ConfirmacaoRelatoRepository confirmacoes;
    private final RotaSalvaRepository rotas;
    private final PasswordEncoder senhas;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;
    private final EntityManager entityManager;

    public UsuarioResponse cadastrar(CadastroRequest dto) {
        String email = normalizarEmail(dto.email());
        if (usuarios.existsByEmail(email)) throw conflito("Este e-mail já está cadastrado.");
        if (dto.senha().getBytes(StandardCharsets.UTF_8).length > 72) throw invalido("A senha deve ter no máximo 72 bytes em UTF-8.");
        Usuario usuario = new Usuario(); usuario.setNome(dto.nome().strip()); usuario.setEmail(email);
        usuario.setSenhaHash(senhas.encode(dto.senha())); usuario.setPapel(Papel.CIDADAO);
        return resposta(usuarios.saveAndFlush(usuario));
    }
    @Transactional(readOnly = true)
    public Usuario usuarioAtual(Long id) { return usuarios.findById(id).orElseThrow(() -> ausente("Usuário não encontrado.")); }
    @Transactional(readOnly = true)
    public UsuarioResponse obterUsuario(Long id) { return resposta(usuarioAtual(id)); }
    public UsuarioResponse atualizarUsuario(Long id, UsuarioRequest dto) {
        Usuario usuario = usuarioAtual(id); String email = normalizarEmail(dto.email());
        if (usuarios.existsByEmailAndIdNot(email, id)) throw conflito("Este e-mail já está cadastrado.");
        usuario.setNome(dto.nome().strip()); usuario.setEmail(email);
        return resposta(usuarios.saveAndFlush(usuario));
    }
    public void excluirUsuario(Long id) {
        Usuario usuario = usuarioAtual(id); perfis.deleteByUsuarioId(id); usuarios.delete(usuario); usuarios.flush();
    }
    @Transactional(readOnly = true)
    public PerfilResponse obterPerfil(Long usuarioId) {
        usuarioAtual(usuarioId);
        return resposta(perfis.findByUsuarioId(usuarioId).orElseThrow(() -> ausente("Crie seu perfil de mobilidade antes de buscar uma rota.")));
    }
    public PerfilResponse salvarPerfil(Long usuarioId, PerfilRequest dto) {
        Usuario usuario = usuarioAtual(usuarioId);
        PerfilMobilidade perfil = perfis.findByUsuarioId(usuarioId).orElseGet(PerfilMobilidade::new);
        perfil.setUsuario(usuario); perfil.setTipo(dto.tipo()); perfil.setCaminhadaMaximaMetros(dto.caminhadaMaximaMetros());
        perfil.setEvitarEscadas(dto.tipo() == TipoMobilidade.CADEIRANTE || dto.evitarEscadas());
        perfil.setPriorizarElevador(dto.priorizarElevador()); perfil.setPriorizarAssento(dto.priorizarAssento());
        return resposta(perfis.saveAndFlush(perfil));
    }
    public void excluirPerfil(Long usuarioId) {
        PerfilMobilidade perfil = perfis.findByUsuarioId(usuarioId).orElseThrow(() -> ausente("Perfil não encontrado.")); perfis.delete(perfil);
    }
    @Transactional(readOnly = true)
    public Page<PontoResponse> listarPontos(Pageable pageable) { return pontos.findAll(pageable).map(this::resposta); }
    @Transactional(readOnly = true)
    public PontoResponse obterPonto(Long id) { return resposta(ponto(id)); }
    public PontoResponse criarPonto(PontoRequest dto) {
        PontoCritico ponto = new PontoCritico(); preencher(ponto, dto); return resposta(pontos.saveAndFlush(ponto));
    }
    public PontoResponse atualizarPonto(Long id, PontoRequest dto) {
        PontoCritico ponto = pontoBloqueado(id);
        if (dto.tipo() != ponto.getTipo() && relatos.existsByPontoCriticoId(id)) throw conflito("Não é possível mudar o tipo de um ponto que possui relatos.");
        if (dto.estado() != EstadoPonto.PROBLEMA && temRelatoAtivo(id)) throw conflito("Resolva os relatos ativos antes de mudar o estado deste ponto.");
        EstadoPonto anterior = ponto.getEstado(); preencher(ponto, dto); pontos.flush();
        if (anterior != ponto.getEstado()) publicar(ponto, "Estado atualizado pelo gestor: " + ponto.getEstado() + ".");
        return resposta(ponto);
    }
    public void excluirPonto(Long id) {
        PontoCritico ponto = pontoBloqueado(id);
        if (relatos.existsByPontoCriticoId(id) || rotas.existsByPontosCriticosId(id)) throw conflito("Este ponto possui relatos ou rotas salvas vinculadas.");
        pontos.delete(ponto); pontos.flush();
    }
    @Transactional(readOnly = true)
    public Page<RelatoResponse> listarRelatos(StatusRelato status, Pageable pageable) {
        return relatos.findByStatusEfetivo(status, clock.instant(), pageable).map(this::resposta);
    }
    @Transactional(readOnly = true)
    public RelatoResponse obterRelato(Long id) { return resposta(relato(id)); }
    public RelatoResponse criarRelato(Long usuarioId, RelatoRequest dto) {
        Usuario usuario = usuarioAtual(usuarioId); PontoCritico ponto = pontoBloqueado(dto.pontoCriticoId());
        validarProblema(ponto.getTipo(), dto.tipoProblema());
        Relato relato = new Relato(); relato.setUsuario(usuario); relato.setPontoCritico(ponto);
        relato.setDescricao(dto.descricao().strip()); relato.setFotoUrl(dto.fotoUrl()); relato.setTipoProblema(dto.tipoProblema());
        Instant agora = clock.instant().truncatedTo(ChronoUnit.MICROS); relato.setCriadoEm(agora); relato.setUltimaConfirmacao(agora); relato.setExpiraEm(agora.plus(VALIDADE));
        relatos.saveAndFlush(relato); atualizarEstado(ponto, EstadoPonto.PROBLEMA, "Novo relato: " + dto.tipoProblema() + ".");
        return resposta(relato);
    }
    public RelatoResponse atualizarRelato(Long usuarioId, Long id, RelatoRequest dto) {
        Relato relato = relatoBloqueado(id); autor(usuarioId, relato); exigirAtivo(relato);
        if (!Objects.equals(dto.pontoCriticoId(), relato.getPontoCritico().getId())) throw conflito("O ponto de um relato não pode ser alterado. Crie outro relato.");
        validarProblema(relato.getPontoCritico().getTipo(), dto.tipoProblema());
        relato.setDescricao(dto.descricao().strip()); relato.setFotoUrl(dto.fotoUrl()); relato.setTipoProblema(dto.tipoProblema());
        // Editar o texto não renova o prazo nem conta como confirmação independente.
        relatos.flush(); return resposta(relato);
    }
    public void excluirRelato(Long usuarioId, Long id) {
        Relato relato = relatoBloqueado(id); autor(usuarioId, relato); exigirAtivo(relato);
        PontoCritico ponto = relato.getPontoCritico(); confirmacoes.deleteByRelatoId(id); relatos.delete(relato); relatos.flush();
        atualizarEstado(ponto, temRelatoAtivo(ponto.getId()) ? EstadoPonto.PROBLEMA : EstadoPonto.DESCONHECIDO, "Relato removido pelo autor; confira as condições antes de sair.");
    }
    public ConfirmacaoResponse confirmar(Long usuarioId, Long id) {
        Usuario usuario = usuarioAtual(usuarioId); Relato relato = relatoBloqueado(id); exigirAtivo(relato);
        if (Objects.equals(usuarioId, relato.getUsuario().getId())) throw conflito("O autor não pode confirmar o próprio relato.");
        if (confirmacoes.existsByRelatoIdAndUsuarioId(id, usuarioId)) return new ConfirmacaoResponse(id, true, relato.getExpiraEm(), confirmacoes.countByRelatoId(id));
        ConfirmacaoRelato confirmacao = new ConfirmacaoRelato(); confirmacao.setUsuario(usuario); confirmacao.setRelato(relato); confirmacoes.saveAndFlush(confirmacao);
        Instant agora = clock.instant().truncatedTo(ChronoUnit.MICROS);
        relato.setUltimaConfirmacao(agora); relato.setExpiraEm(agora.plus(VALIDADE));
        atualizarEstado(relato.getPontoCritico(), EstadoPonto.PROBLEMA, "Problema confirmado pela comunidade.");
        return new ConfirmacaoResponse(id, false, relato.getExpiraEm(), confirmacoes.countByRelatoId(id));
    }
    public RelatoResponse resolver(Long id) {
        Relato relato = relatoBloqueado(id);
        if (relato.getStatus() == StatusRelato.RESOLVIDO) return resposta(relato);
        relato.setStatus(StatusRelato.RESOLVIDO); relatos.flush();
        PontoCritico ponto = relato.getPontoCritico();
        atualizarEstado(ponto, temRelatoAtivo(ponto.getId()) ? EstadoPonto.PROBLEMA : EstadoPonto.OPERACIONAL, "Relato resolvido pelo gestor.");
        return resposta(relato);
    }
    @Scheduled(fixedDelayString = "${rotaleve.relatos.expiracao-delay-ms:60000}")
    public void expirarRelatos() {
        Instant agora = clock.instant();
        for (Relato candidato : relatos.findByStatusAndExpiraEmBefore(StatusRelato.ATIVO, agora)) {
            PontoCritico ponto = pontoBloqueado(candidato.getPontoCritico().getId());
            entityManager.refresh(candidato);
            if (candidato.getStatus() != StatusRelato.ATIVO || candidato.getExpiraEm().isAfter(agora)) continue;
            candidato.setStatus(StatusRelato.EXPIRADO); relatos.flush();
            atualizarEstado(ponto, temRelatoAtivo(ponto.getId()) ? EstadoPonto.PROBLEMA : EstadoPonto.DESCONHECIDO,
                "Relato expirou sem novas confirmações; funcionamento ainda não verificado.");
        }
    }
    private Relato relatoBloqueado(Long id) {
        Relato relato = relato(id); pontoBloqueado(relato.getPontoCritico().getId()); entityManager.refresh(relato); return relato;
    }
    private PontoCritico ponto(Long id) { return pontos.findById(id).orElseThrow(() -> ausente("Ponto crítico não encontrado.")); }
    private PontoCritico pontoBloqueado(Long id) { return pontos.findByIdForUpdate(id).orElseThrow(() -> ausente("Ponto crítico não encontrado.")); }
    private Relato relato(Long id) { return relatos.findById(id).orElseThrow(() -> ausente("Relato não encontrado.")); }
    private boolean temRelatoAtivo(Long id) { return relatos.existsByPontoCriticoIdAndStatusAndExpiraEmAfter(id, StatusRelato.ATIVO, clock.instant()); }
    private void autor(Long usuarioId, Relato relato) { if (!Objects.equals(usuarioId, relato.getUsuario().getId())) throw new AccessDeniedException("Relato de outro usuário."); }
    private void exigirAtivo(Relato relato) { if (statusEfetivo(relato) != StatusRelato.ATIVO) throw conflito("O relato está resolvido ou expirado."); }
    private void preencher(PontoCritico ponto, PontoRequest dto) {
        ponto.setNome(dto.nome().strip()); ponto.setTipo(dto.tipo()); ponto.setLatitude(dto.latitude()); ponto.setLongitude(dto.longitude());
        ponto.setEstado(dto.estado()); ponto.setUltimaVerificacao(dto.estado() == EstadoPonto.DESCONHECIDO ? null : clock.instant());
    }
    private void atualizarEstado(PontoCritico ponto, EstadoPonto estado, String mensagem) {
        EstadoPonto anterior = ponto.getEstado(); ponto.setEstado(estado); ponto.setUltimaVerificacao(clock.instant());
        if (anterior != estado) publicar(ponto, mensagem);
    }
    private void publicar(PontoCritico ponto, String mensagem) { eventos.publishEvent(new PontoAlteradoEvent(ponto.getId(), ponto.getNome() + ": " + mensagem)); }
    private void validarProblema(TipoPonto ponto, TipoProblema problema) {
        boolean valido = switch (problema) {
            case ELEVADOR_QUEBRADO -> ponto == TipoPonto.ELEVADOR;
            case SEM_COBERTURA -> ponto == TipoPonto.PONTO_ONIBUS;
            case BURACO -> ponto == TipoPonto.CALCADA || ponto == TipoPonto.RAMPA;
            case DEGRAU -> ponto == TipoPonto.ESCADA || ponto == TipoPonto.CALCADA || ponto == TipoPonto.RAMPA;
            case OBSTACULO -> true;
        };
        if (!valido) throw invalido("Este tipo de problema não corresponde ao tipo do ponto crítico.");
    }
    private StatusRelato statusEfetivo(Relato relato) {
        return relato.getStatus() == StatusRelato.ATIVO && !relato.getExpiraEm().isAfter(clock.instant()) ? StatusRelato.EXPIRADO : relato.getStatus();
    }
    private UsuarioResponse resposta(Usuario u) { return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPapel()); }
    private PerfilResponse resposta(PerfilMobilidade p) { return new PerfilResponse(p.getId(), p.getTipo(), p.getCaminhadaMaximaMetros(), p.isEvitarEscadas(), p.isPriorizarElevador(), p.isPriorizarAssento()); }
    private PontoResponse resposta(PontoCritico p) {
        EstadoPonto estado = p.getEstado();
        if (temRelatoAtivo(p.getId())) estado = EstadoPonto.PROBLEMA;
        else if (p.getUltimaVerificacao() == null || !p.getUltimaVerificacao().plus(VALIDADE_VERIFICACAO).isAfter(clock.instant())) estado = EstadoPonto.DESCONHECIDO;
        return new PontoResponse(p.getId(), p.getNome(), p.getTipo(), p.getLatitude(), p.getLongitude(), estado, p.getUltimaVerificacao());
    }
    private RelatoResponse resposta(Relato r) { return new RelatoResponse(r.getId(), r.getUsuario().getId(), r.getPontoCritico().getId(), r.getDescricao(), r.getFotoUrl(), r.getTipoProblema(), statusEfetivo(r), r.getCriadoEm(), r.getUltimaConfirmacao(), r.getExpiraEm(), confirmacoes.countByRelatoId(r.getId())); }
    private String normalizarEmail(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    private RegraNegocioException ausente(String mensagem) { return new RegraNegocioException(HttpStatus.NOT_FOUND, mensagem); }
    private RegraNegocioException conflito(String mensagem) { return new RegraNegocioException(HttpStatus.CONFLICT, mensagem); }
    private RegraNegocioException invalido(String mensagem) { return new RegraNegocioException(HttpStatus.BAD_REQUEST, mensagem); }
}
