package br.com.fiap.rotaleve.routes;

import br.com.fiap.rotaleve.repository.*;
import com.fasterxml.jackson.databind.*;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:rotas-tests;DB_CLOSE_DELAY=-1", "rotaleve.outbox.intervalo-ms=3600000"})
@AutoConfigureMockMvc @Transactional
class RotasFluxosTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ConsultaRotaRepository consultas;
    @Autowired PontoCriticoRepository pontos;
    @Autowired NotificacaoRepository notificacoes;
    @Autowired RotaSalvaRepository salvas;
    @Autowired UsuarioRepository usuarios;
    String cidadao, gestor;
    @BeforeEach void autenticar() throws Exception {
        cidadao = login("cidadao@rotaleve.local", "Cidadao@123"); gestor = login("gestor@rotaleve.local", "Gestor@123");
    }
    String login(String email, String senha) throws Exception {
        return body(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("email",email,"senha",senha)))).andExpect(status().isOk()).andReturn()).get("accessToken").asText();
    }
    JsonNode body(MvcResult result) throws Exception { return mapper.readTree(result.getResponse().getContentAsString()); }
    JsonNode buscar() throws Exception {
        return body(mvc.perform(post("/api/v1/rotas/busca").header("Authorization","Bearer "+cidadao)
            .contentType(MediaType.APPLICATION_JSON).content("{\"origem\":\"Praça da Sé\",\"destino\":\"Hospital das Clínicas\"}"))
            .andExpect(status().isOk()).andReturn());
    }
    JsonNode salvar(JsonNode busca, String rotaId) throws Exception {
        return body(mvc.perform(post("/api/v1/rotas/salvas").header("Authorization","Bearer "+cidadao)
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("consultaId",busca.get("consultaId").asText(),"rotaId",rotaId,"nome","Pré-natal"))))
            .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn());
    }
    @Test void buscaRanqueiaESeparaRotasIncompativeis() throws Exception {
        var b = buscar(); assertThat(b.get("demonstracao").asBoolean()).isTrue();
        assertThat(b.get("temOpcaoCompativel").asBoolean()).isTrue();
        assertThat(b.at("/rotas/0/rota/id").asText()).isEqualTo("demo-se");
        assertThat(b.at("/rotas/2/compatibilidade").asText()).isEqualTo("INCOMPATIVEL");
    }
    @Test void fluxoSalvarListarExplicarEditarExcluir() throws Exception {
        var salva = salvar(buscar(), "demo-se"); long id=salva.get("id").asLong();
        mvc.perform(get("/api/v1/rotas/salvas?size=1").header("Authorization","Bearer "+cidadao))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(id));
        mvc.perform(post("/api/v1/rotas/salvas/{id}/explicacao",id).header("Authorization","Bearer "+cidadao))
            .andExpect(status().isOk()).andExpect(jsonPath("$.fonte").value("REGRAS_LOCAIS_SEM_LLM"));
        mvc.perform(put("/api/v1/rotas/salvas/{id}",id).header("Authorization","Bearer "+cidadao)
            .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Consulta\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Consulta"));
        mvc.perform(delete("/api/v1/rotas/salvas/{id}",id).header("Authorization","Bearer "+cidadao)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/rotas/salvas/{id}",id).header("Authorization","Bearer "+cidadao)).andExpect(status().isNotFound());
    }
    @Test void rotaSalvaPrivadaInclusiveParaGestor() throws Exception {
        var salva=salvar(buscar(),"demo-se");
        mvc.perform(get("/api/v1/rotas/salvas/{id}",salva.get("id").asLong()).header("Authorization","Bearer "+gestor)).andExpect(status().isNotFound());
    }
    @Test void naoAceitaSnapshotInventadoPeloCliente() throws Exception {
        var busca=buscar();
        mvc.perform(post("/api/v1/rotas/salvas").header("Authorization","Bearer "+cidadao).contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("consultaId",busca.get("consultaId").asText(),"rotaId","inventada","nome","Falsa"))))
            .andExpect(status().isNotFound());
    }
    @Test void buscaExpiradaExigeNovaPesquisa() throws Exception {
        var busca=buscar(); var id=java.util.UUID.fromString(busca.get("consultaId").asText());
        var registro=consultas.findById(id).orElseThrow(); registro.setExpiraEm(Instant.EPOCH); consultas.saveAndFlush(registro);
        mvc.perform(post("/api/v1/rotas/salvas").header("Authorization","Bearer "+cidadao).contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("consultaId",id,"rotaId","demo-se","nome","Velha"))))
            .andExpect(status().isGone());
    }
    @Test void novoProblemaMudaAvaliacaoDaRotaJaSalva() throws Exception {
        var salva=salvar(buscar(),"demo-se");
        long ponto=pontos.findAll().stream().filter(p->p.getNome().equals("Elevador Sé (demo)")).findFirst().orElseThrow().getId();
        mvc.perform(post("/api/v1/relatos").header("Authorization","Bearer "+cidadao).contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("pontoCriticoId",ponto,"descricao","Elevador está parado no acesso principal", "fotoUrl","https://example.org/elevador.jpg","tipoProblema","ELEVADOR_QUEBRADO"))))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/rotas/salvas/{id}",salva.get("id").asLong()).header("Authorization","Bearer "+cidadao))
            .andExpect(status().isOk()).andExpect(jsonPath("$.avaliacao.compatibilidade").value("INCOMPATIVEL"));
        assertThat(buscar().at("/rotas/0/rota/id").asText()).isEqualTo("demo-republica");
    }
    @Test void demoNaoInventaTrajetoParaEnderecoArbitrario() throws Exception {
        mvc.perform(post("/api/v1/rotas/busca").header("Authorization","Bearer "+cidadao).contentType(MediaType.APPLICATION_JSON)
            .content("{\"origem\":\"Rio de Janeiro\",\"destino\":\"Curitiba\"}"))
            .andExpect(status().isUnprocessableEntity());
    }
    @Test void informaQuandoNenhumaOpcaoAtendeAoPerfil() throws Exception {
        mvc.perform(put("/api/v1/perfis/me").header("Authorization","Bearer "+cidadao).contentType(MediaType.APPLICATION_JSON)
            .content("{\"tipo\":\"TEMPORARIA\",\"caminhadaMaximaMetros\":50,\"evitarEscadas\":true,\"priorizarElevador\":true,\"priorizarAssento\":true}"))
            .andExpect(status().isOk());
        var resposta=buscar();
        assertThat(resposta.get("temOpcaoCompativel").asBoolean()).isFalse();
        for(var rota:resposta.get("rotas")) assertThat(rota.get("compatibilidade").asText()).isEqualTo("INCOMPATIVEL");
    }
    @Test void buscaSemPerfilOrientaCadastro() throws Exception {
        mvc.perform(delete("/api/v1/perfis/me").header("Authorization","Bearer "+cidadao)).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/rotas/busca").header("Authorization","Bearer "+cidadao).contentType(MediaType.APPLICATION_JSON)
            .content("{\"origem\":\"Praça da Sé\",\"destino\":\"Hospital das Clínicas\"}"))
            .andExpect(status().isUnprocessableEntity());
    }
    @Test void excluirRotaTambemRemoveSeusAvisos() throws Exception {
        var salva=salvar(buscar(),"demo-se"); long id=salva.get("id").asLong();
        var aviso=new br.com.fiap.rotaleve.model.Notificacao();
        aviso.setUsuario(usuarios.findByEmail("cidadao@rotaleve.local").orElseThrow());
        aviso.setRotaSalva(salvas.findById(id).orElseThrow()); aviso.setEventoId("evento-teste-exclusao");
        aviso.setMensagem("Condição mudou"); aviso.setCriadaEm(Instant.now()); notificacoes.saveAndFlush(aviso);
        mvc.perform(delete("/api/v1/rotas/salvas/{id}",id).header("Authorization","Bearer "+cidadao)).andExpect(status().isNoContent());
        notificacoes.flush(); assertThat(notificacoes.existsById(aviso.getId())).isFalse();
    }
}
