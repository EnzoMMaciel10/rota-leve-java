package br.com.fiap.rotaleve.routes;

import br.com.fiap.rotaleve.model.*;
import br.com.fiap.rotaleve.model.Tipos.*;
import br.com.fiap.rotaleve.repository.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static br.com.fiap.rotaleve.routes.RotasDtos.Compatibilidade.*;

class AcessibilidadeServiceTest {
    final PontoCriticoRepository pontos = mock(PontoCriticoRepository.class);
    final RelatoRepository relatos = mock(RelatoRepository.class);
    final Instant agora = Instant.parse("2026-11-16T12:00:00Z");
    final AcessibilidadeService service = new AcessibilidadeService(pontos, relatos, Clock.fixed(agora, ZoneOffset.UTC));
    PerfilMobilidade perfil;
    @BeforeEach void setup() {
        perfil = new PerfilMobilidade(); perfil.setTipo(TipoMobilidade.GESTANTE);
        perfil.setCaminhadaMaximaMetros(600); perfil.setEvitarEscadas(true);
        perfil.setPriorizarElevador(true); perfil.setPriorizarAssento(true);
    }
    RotasDtos.Candidata rota(String id, int caminhada, Boolean escadas, Boolean embarque, List<Long> ids) {
        return new RotasDtos.Candidata(id, "A", "B", "Teste", "DEMO_SINTETICA", 1000, caminhada, escadas, embarque, true, true, ids, null);
    }
    @Test void escadasBloqueiamMesmoSeRotaMaisRapida() {
        var result = service.ordenar(List.of(rota("escada", 50, true, true, List.of()), rota("plana",400,false,true,List.of())), perfil);
        assertThat(result.get(0).rota().id()).isEqualTo("plana");
        assertThat(result.get(1).compatibilidade()).isEqualTo(INCOMPATIVEL);
    }
    @Test void caminhadaAcimaDoLimiteIncompativel() {
        assertThat(service.avaliar(rota("longa",601,false,true,List.of()),perfil).compatibilidade()).isEqualTo(INCOMPATIVEL);
    }
    @Test void dadoDesconhecidoNaoViraAcessibilidadeConfirmada() {
        assertThat(service.avaliar(rota("desconhecida",300,null,null,List.of()),perfil).compatibilidade()).isEqualTo(DADOS_INSUFICIENTES);
    }
    @Test void cadeiranteNaoPodeDesligarRestricaoFisicaDeEscadas() {
        perfil.setTipo(TipoMobilidade.CADEIRANTE); perfil.setEvitarEscadas(false);
        assertThat(service.avaliar(rota("escada",50,true,true,List.of()),perfil).compatibilidade()).isEqualTo(INCOMPATIVEL);
        assertThat(service.avaliar(rota("embarque",50,false,null,List.of()),perfil).compatibilidade()).isEqualTo(DADOS_INSUFICIENTES);
    }
    @Test void elevadorQuebradoReordenaAlternativas() {
        var ponto = new PontoCritico(); ponto.setId(1L); ponto.setNome("Elevador");
        ponto.setEstado(EstadoPonto.OPERACIONAL); ponto.setUltimaVerificacao(agora);
        when(pontos.findById(1L)).thenReturn(Optional.of(ponto));
        when(relatos.existsByPontoCriticoIdAndStatusAndExpiraEmAfter(1L,StatusRelato.ATIVO,agora)).thenReturn(true);
        var result = service.ordenar(List.of(rota("quebrada",100,false,true,List.of(1L)),rota("alternativa",400,false,true,List.of())),perfil);
        assertThat(result.get(0).rota().id()).isEqualTo("alternativa");
        assertThat(result.get(1).compatibilidade()).isEqualTo(INCOMPATIVEL);
    }
    @Test void verificacaoVelhaNaoGaranteElevadorFuncionando() {
        var ponto = new PontoCritico(); ponto.setId(1L); ponto.setNome("Elevador"); ponto.setEstado(EstadoPonto.OPERACIONAL);
        ponto.setUltimaVerificacao(agora.minus(Duration.ofHours(25))); when(pontos.findById(1L)).thenReturn(Optional.of(ponto));
        assertThat(service.avaliar(rota("velha",100,false,true,List.of(1L)),perfil).compatibilidade()).isEqualTo(DADOS_INSUFICIENTES);
    }
}
