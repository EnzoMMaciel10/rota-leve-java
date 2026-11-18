package br.com.fiap.rotaleve.routes;

import br.com.fiap.rotaleve.model.*;
import br.com.fiap.rotaleve.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @RequiredArgsConstructor
@Transactional(readOnly=true)
public class RotasService {
    private final ProvedorRotas provedor;
    private final AcessibilidadeService acessibilidade;
    private final ConsultaRotaRepository consultas;
    private final RotaSalvaRepository salvas;
    private final UsuarioRepository usuarios;
    private final PerfilMobilidadeRepository perfis;
    private final PontoCriticoRepository pontos;
    private final NotificacaoRepository notificacoes;
    private final ObjectMapper mapper;
    private final Clock clock;

    @Transactional
    public RotasDtos.BuscaResponse buscar(long usuarioId, RotasDtos.BuscaRequest request) {
        PerfilMobilidade perfil = perfil(usuarioId);
        var candidatas = provedor.buscar(request.origem().trim(), request.destino().trim());
        var avaliadas = acessibilidade.ordenar(candidatas, perfil);
        var consulta = new ConsultaRota();
        consulta.setId(UUID.randomUUID()); consulta.setUsuarioId(usuarioId);
        consulta.setExpiraEm(clock.instant().plus(Duration.ofMinutes(30)));
        consulta.setCandidatasJson(json(candidatas)); consultas.save(consulta);
        return new RotasDtos.BuscaResponse(consulta.getId(), consulta.getExpiraEm(), provedor.nome(),
            "DEMO_SINTETICA".equals(provedor.nome()), avaliadas.stream().anyMatch(a ->
                a.compatibilidade() == RotasDtos.Compatibilidade.COMPATIVEL_COM_DADOS_CONHECIDOS), avaliadas);
    }
    @Transactional
    public RotasDtos.SalvaResponse salvar(long usuarioId, RotasDtos.SalvarRequest request) {
        var consulta = consultas.findById(request.consultaId())
            .filter(c -> c.getUsuarioId().equals(usuarioId)).orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Busca não encontrada"));
        if (!consulta.getExpiraEm().isAfter(clock.instant())) throw erro(HttpStatus.GONE, "Busca expirada; pesquise novamente");
        List<RotasDtos.Candidata> candidatas;
        try { candidatas = mapper.readValue(consulta.getCandidatasJson(), new TypeReference<>() {}); }
        catch (Exception ex) { throw new IllegalStateException("Não foi possível ler a busca persistida", ex); }
        var candidata = candidatas.stream().filter(c -> c.id().equals(request.rotaId())).findFirst()
            .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Alternativa não encontrada nesta busca"));
        var rota = new RotaSalva();
        rota.setUsuario(usuarios.findById(usuarioId).orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Usuário não encontrado")));
        rota.setNome(request.nome().trim()); rota.setOrigem(candidata.origem()); rota.setDestino(candidata.destino());
        rota.setSnapshotJson(json(candidata)); rota.setCriadaEm(clock.instant());
        var refs = new HashSet<PontoCritico>();
        for (Long id : candidata.pontosCriticos()) refs.add(pontos.findById(id)
            .orElseThrow(() -> erro(HttpStatus.CONFLICT, "Ponto da rota foi removido; pesquise novamente")));
        rota.setPontosCriticos(refs);
        return resposta(salvas.save(rota), perfil(usuarioId));
    }
    public Page<RotasDtos.SalvaResponse> listar(long usuarioId, Pageable pageable) {
        var perfil = perfil(usuarioId);
        return salvas.findByUsuarioId(usuarioId, pageable).map(r -> resposta(r, perfil));
    }
    public RotasDtos.SalvaResponse detalhar(long usuarioId, long id) { return resposta(propria(usuarioId, id), perfil(usuarioId)); }
    @Transactional public RotasDtos.SalvaResponse renomear(long usuarioId, long id, RotasDtos.NomeRequest request) {
        var rota = propria(usuarioId, id); rota.setNome(request.nome().trim()); return resposta(rota, perfil(usuarioId));
    }
    @Transactional public void excluir(long usuarioId, long id) {
        var rota = propria(usuarioId, id);
        notificacoes.deleteByRotaSalvaId(id);
        salvas.delete(rota);
    }
    @Scheduled(fixedDelayString="${rotaleve.consultas.limpeza-ms:3600000}")
    @Transactional public void limparConsultas() { consultas.deleteByExpiraEmBefore(clock.instant()); }
    private RotaSalva propria(long usuarioId, long id) {
        return salvas.findById(id).filter(r -> r.getUsuario().getId().equals(usuarioId))
            .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Rota não encontrada"));
    }
    private PerfilMobilidade perfil(long usuarioId) {
        return perfis.findByUsuarioId(usuarioId)
            .orElseThrow(() -> erro(HttpStatus.UNPROCESSABLE_ENTITY, "Cadastre seu perfil de mobilidade antes de buscar ou avaliar rotas"));
    }
    private RotasDtos.SalvaResponse resposta(RotaSalva rota, PerfilMobilidade perfil) {
        try {
            var candidata = mapper.readValue(rota.getSnapshotJson(), RotasDtos.Candidata.class);
            return new RotasDtos.SalvaResponse(rota.getId(), rota.getNome(), rota.getOrigem(), rota.getDestino(),
                rota.getCriadaEm(), acessibilidade.avaliar(candidata, perfil));
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException("Snapshot inválido", ex); }
    }
    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception ex) { throw new IllegalStateException("Falha de serialização", ex); }
    }
    private ResponseStatusException erro(HttpStatus status, String mensagem) { return new ResponseStatusException(status, mensagem); }
}
