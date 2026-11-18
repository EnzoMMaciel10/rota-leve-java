package br.com.fiap.rotaleve.routes;

import java.time.*;
import java.util.*;
import br.com.fiap.rotaleve.model.PerfilMobilidade;
import br.com.fiap.rotaleve.model.Tipos.*;
import br.com.fiap.rotaleve.repository.*;
import static br.com.fiap.rotaleve.routes.RotasDtos.Compatibilidade.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class AcessibilidadeService {
    private final PontoCriticoRepository pontos;
    private final RelatoRepository relatos;
    private final Clock clock;
    public RotasDtos.Avaliacao avaliar(RotasDtos.Candidata rota, PerfilMobilidade perfil) {
        boolean bloqueada = false, incompleta = false;
        int score = 100;
        List<String> motivos = new ArrayList<>();
        boolean semEscadas = perfil.isEvitarEscadas() || perfil.getTipo() == TipoMobilidade.CADEIRANTE;
        if (rota.caminhadaMetros() > perfil.getCaminhadaMaximaMetros()) {
            bloqueada = true; score -= 50; motivos.add("Caminhada excede o limite do perfil");
        } else {
            score -= Math.min(15, 15 * rota.caminhadaMetros() / Math.max(1, perfil.getCaminhadaMaximaMetros()));
        }
        if (semEscadas && Boolean.TRUE.equals(rota.temEscadas())) {
            bloqueada = true; score -= 60; motivos.add("Há escadas incompatíveis com o perfil");
        }
        if (rota.temEscadas() == null) { incompleta = true; score -= 20; motivos.add("Presença de escadas não verificada"); }
        if (perfil.getTipo() == TipoMobilidade.CADEIRANTE && !Boolean.TRUE.equals(rota.embarqueAcessivel())) {
            if (Boolean.FALSE.equals(rota.embarqueAcessivel())) { bloqueada = true; score -= 60; motivos.add("Embarque inacessível"); }
            else { incompleta = true; score -= 20; motivos.add("Embarque acessível não verificado"); }
        }
        if (perfil.isPriorizarAssento() && !Boolean.TRUE.equals(rota.temAssento())) {
            score -= 10; motivos.add(rota.temAssento() == null ? "Assentos não verificados" : "Sem assento informado");
        }
        if (!Boolean.TRUE.equals(rota.temCobertura())) { score -= 5; motivos.add("Cobertura ausente ou não verificada"); }
        Instant agora = clock.instant();
        for (Long id : rota.pontosCriticos()) {
            var ponto = pontos.findById(id).orElse(null);
            if (ponto == null) { incompleta = true; score -= 20; motivos.add("Ponto crítico sem cadastro atualizado"); continue; }
            boolean relatoAtivo = relatos.existsByPontoCriticoIdAndStatusAndExpiraEmAfter(id, StatusRelato.ATIVO, agora);
            boolean verificacaoRecente = ponto.getUltimaVerificacao() != null
                && !ponto.getUltimaVerificacao().isBefore(agora.minus(Duration.ofHours(24)));
            if (relatoAtivo || (ponto.getEstado() == EstadoPonto.PROBLEMA && verificacaoRecente)) {
                bloqueada = true; score -= 60; motivos.add("Problema no ponto: " + ponto.getNome());
            } else if (ponto.getEstado() != EstadoPonto.OPERACIONAL || !verificacaoRecente) {
                incompleta = true; score -= 20; motivos.add("Condição não verificada recentemente: " + ponto.getNome());
            }
        }
        if (perfil.isPriorizarElevador() && rota.pontosCriticos().isEmpty() && !Boolean.FALSE.equals(rota.temEscadas())) {
            incompleta = true; motivos.add("Elevadores da rota não verificados");
        }
        if (!"DEMO_SINTETICA".equals(rota.fonte())) {
            incompleta = true; motivos.add("Acessibilidade dos trechos ainda precisa de enriquecimento local");
        }
        if ("DEMO_SINTETICA".equals(rota.fonte())) motivos.add("Dados sintéticos de demonstração; não representam condições reais");
        if (motivos.isEmpty()) motivos.add("Compatível com os dados disponíveis do perfil");
        return new RotasDtos.Avaliacao(rota, Math.max(0, Math.min(100, score)),
            bloqueada ? INCOMPATIVEL : incompleta ? DADOS_INSUFICIENTES : COMPATIVEL_COM_DADOS_CONHECIDOS,
            List.copyOf(motivos), agora);
    }
    public List<RotasDtos.Avaliacao> ordenar(List<RotasDtos.Candidata> candidatas, PerfilMobilidade perfil) {
        return candidatas.stream().map(c -> avaliar(c, perfil)).sorted(
            Comparator.comparingInt((RotasDtos.Avaliacao a) -> switch(a.compatibilidade()) {
                case COMPATIVEL_COM_DADOS_CONHECIDOS -> 0; case DADOS_INSUFICIENTES -> 1; case INCOMPATIVEL -> 2;
            }).thenComparing(Comparator.comparingInt(RotasDtos.Avaliacao::score).reversed())
              .thenComparingInt(a -> a.rota().duracaoSegundos())).toList();
    }
}
