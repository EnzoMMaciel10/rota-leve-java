package br.com.fiap.rotaleve;

import br.com.fiap.rotaleve.model.*;
import br.com.fiap.rotaleve.model.Tipos.*;
import br.com.fiap.rotaleve.repository.*;
import br.com.fiap.rotaleve.service.CoreService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercita HTTP com JWT realmente assinado, banco e regras de negócio. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:coretests;MODE=Oracle;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop", "rotaleve.rabbit.enabled=false",
    "rotaleve.ai.enabled=false", "spring.ai.model.chat=none",
    "rotaleve.relatos.expiracao-delay-ms=86400000"
})
@ActiveProfiles("demo") @AutoConfigureMockMvc @Transactional
class CoreFluxosTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired RelatoRepository relatos;
    @Autowired PontoCriticoRepository pontos;
    @Autowired ConfirmacaoRelatoRepository confirmacoes;
    @Autowired PasswordEncoder passwords;
    @Autowired CoreService service;
    @Autowired Clock clock;

    @Test void autenticaJwtRealEImpedeCidadaoDeGerenciarPontos() throws Exception {
        Usuario cidadao = usuario(Papel.CIDADAO); String token = login(cidadao);
        mvc.perform(get("/api/v1/usuarios/me")).andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", cidadao.getEmail(), "senha", "senhaerrada"))))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(cidadao.getId()))
            .andExpect(jsonPath("$.senhaHash").doesNotExist());
        String body = json.writeValueAsString(Map.of("nome", "Ponto novo", "tipo", "ELEVADOR", "latitude", -23.5, "longitude", -46.6, "estado", "OPERACIONAL"));
        mvc.perform(post("/api/v1/pontos").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/pontos").header("Authorization", bearer(usuario(Papel.GESTOR))).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("OPERACIONAL"));
        mvc.perform(get("/api/v1/pontos").header("Authorization", "Bearer " + token.substring(0, token.length() - 10) + "invalid000"))
            .andExpect(status().isUnauthorized());
    }

    @Test void cadastroNormalizaEmailENaoPermiteSolicitarPapelGestor() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        String body = json.writeValueAsString(Map.of("nome", "Pessoa teste", "email", email.toUpperCase(), "senha", "Senha@123", "papel", "GESTOR"));
        mvc.perform(post("/api/v1/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.papel").value("CIDADAO"))
            .andExpect(jsonPath("$.email").value(email)).andExpect(jsonPath("$.senhaHash").doesNotExist());
        mvc.perform(post("/api/v1/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("nome", "Outro nome", "email", email, "senha", "Senha@123"))))
            .andExpect(status().isConflict());
    }

    @Test void validaPerfilFotoTiposECamposObrigatoriosComProblemDetail() throws Exception {
        String token = bearer(usuario(Papel.CIDADAO));
        mvc.perform(put("/api/v1/perfis/me").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.tipo").exists());
        PontoCritico ponto = ponto();
        mvc.perform(post("/api/v1/relatos").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("pontoCriticoId", ponto.getId(), "descricao", "Elevador sem funcionamento", "tipoProblema", "ELEVADOR_QUEBRADO"))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.fotoUrl").exists());
        mvc.perform(get("/api/v1/pontos/abc").header("Authorization", token)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/pontos/-1").header("Authorization", token)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/relatos?status=INVALIDO").header("Authorization", token)).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/perfis/me").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest());
    }

    @Test void perfilCadeiranteSempreEvitaEscadasECRUDDoPerfilFunciona() throws Exception {
        String token = bearer(usuario(Papel.CIDADAO));
        mvc.perform(get("/api/v1/perfis/me").header("Authorization", token)).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/perfis/me").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("tipo", "CADEIRANTE", "caminhadaMaximaMetros", 500,
                "evitarEscadas", false, "priorizarElevador", true, "priorizarAssento", false))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.evitarEscadas").value(true));
        mvc.perform(delete("/api/v1/perfis/me").header("Authorization", token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/perfis/me").header("Authorization", token)).andExpect(status().isNotFound());
    }

    @Test void autorNaoConfirmaProprioRelatoEConfirmacaoRepetidaNaoRenovaValidade() throws Exception {
        Usuario autor = usuario(Papel.CIDADAO); PontoCritico ponto = ponto();
        JsonNode criado = criarRelato(autor, ponto); long id = criado.get("id").asLong();
        mvc.perform(post("/api/v1/relatos/{id}/confirmacoes", id).header("Authorization", bearer(autor))).andExpect(status().isConflict());
        String outro = bearer(usuario(Papel.CIDADAO));
        String primeiro = mvc.perform(post("/api/v1/relatos/{id}/confirmacoes", id).header("Authorization", outro))
            .andExpect(status().isOk()).andExpect(jsonPath("$.jaConfirmado").value(false)).andExpect(jsonPath("$.confirmacoes").value(1))
            .andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/v1/relatos/{id}/confirmacoes", id).header("Authorization", outro))
            .andExpect(status().isOk()).andExpect(jsonPath("$.jaConfirmado").value(true)).andExpect(jsonPath("$.confirmacoes").value(1))
            .andExpect(jsonPath("$.expiraEm").value(json.readTree(primeiro).get("expiraEm").asText()));
        assertThat(confirmacoes.countByRelatoId(id)).isEqualTo(1);
    }

    @Test void impedeOutroUsuarioDeEditarOuExcluirRelato() throws Exception {
        Usuario autor = usuario(Papel.CIDADAO); PontoCritico ponto = ponto(); long id = criarRelato(autor, ponto).get("id").asLong();
        String outro = bearer(usuario(Papel.CIDADAO));
        mvc.perform(put("/api/v1/relatos/{id}", id).header("Authorization", outro).contentType(MediaType.APPLICATION_JSON).content(relatoJson(ponto)))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/relatos/{id}", id).header("Authorization", outro)).andExpect(status().isForbidden());
        assertThat(relatos.existsById(id)).isTrue();
    }

    @Test void expiracaoNaoDeclaraElevadorOperacionalENaoPermiteReativarRelato() throws Exception {
        Usuario autor = usuario(Papel.CIDADAO); PontoCritico ponto = ponto(); long id = criarRelato(autor, ponto).get("id").asLong();
        Relato relato = relatos.findById(id).orElseThrow(); relato.setExpiraEm(clock.instant().minusSeconds(1)); relatos.flush();
        service.expirarRelatos();
        assertThat(relatos.findById(id).orElseThrow().getStatus()).isEqualTo(StatusRelato.EXPIRADO);
        assertThat(pontos.findById(ponto.getId()).orElseThrow().getEstado()).isEqualTo(EstadoPonto.DESCONHECIDO);
        String token = bearer(usuario(Papel.CIDADAO));
        mvc.perform(post("/api/v1/relatos/{id}/confirmacoes", id).header("Authorization", token)).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/relatos?status=ATIVO").header("Authorization", token)).andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get("/api/v1/relatos?status=EXPIRADO").header("Authorization", token)).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(id));
    }

    @Test void resolverUmRelatoNaoOcultaOutroProblemaAtivo() throws Exception {
        Usuario autor = usuario(Papel.CIDADAO); PontoCritico ponto = ponto();
        long primeiro = criarRelato(autor, ponto).get("id").asLong(); long segundo = criarRelato(autor, ponto).get("id").asLong();
        mvc.perform(post("/api/v1/relatos/{id}/resolver", primeiro).header("Authorization", bearer(autor))).andExpect(status().isForbidden());
        String gestor = bearer(usuario(Papel.GESTOR));
        mvc.perform(post("/api/v1/relatos/{id}/resolver", primeiro).header("Authorization", gestor)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVIDO"));
        assertThat(pontos.findById(ponto.getId()).orElseThrow().getEstado()).isEqualTo(EstadoPonto.PROBLEMA);
        mvc.perform(post("/api/v1/relatos/{id}/resolver", segundo).header("Authorization", gestor)).andExpect(status().isOk());
        assertThat(pontos.findById(ponto.getId()).orElseThrow().getEstado()).isEqualTo(EstadoPonto.OPERACIONAL);
    }

    @Test void crudDePontoComValidacaoEPaginacao() throws Exception {
        String gestor = bearer(usuario(Papel.GESTOR));
        String body = json.writeValueAsString(Map.of("nome", "Calçada de teste", "tipo", "CALCADA", "latitude", -23.5, "longitude", -46.6, "estado", "DESCONHECIDO"));
        String criado = mvc.perform(post("/api/v1/pontos").header("Authorization", gestor).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = json.readTree(criado).get("id").asLong();
        mvc.perform(get("/api/v1/pontos?page=0&size=2").header("Authorization", gestor)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2));
        mvc.perform(put("/api/v1/pontos/{id}", id).header("Authorization", gestor).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/pontos/{id}", id).header("Authorization", gestor)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/pontos/{id}", id).header("Authorization", gestor)).andExpect(status().isNotFound());
    }

    private Usuario usuario(Papel papel) {
        Usuario usuario = new Usuario(); usuario.setNome("Pessoa de teste"); usuario.setEmail(UUID.randomUUID() + "@example.test");
        usuario.setSenhaHash(passwords.encode("Senha@123")); usuario.setPapel(papel); return usuarios.saveAndFlush(usuario);
    }
    private PontoCritico ponto() {
        PontoCritico ponto = new PontoCritico(); ponto.setNome("Elevador de teste " + UUID.randomUUID()); ponto.setTipo(TipoPonto.ELEVADOR);
        ponto.setLatitude(-23.5); ponto.setLongitude(-46.6); ponto.setEstado(EstadoPonto.OPERACIONAL); ponto.setUltimaVerificacao(clock.instant());
        return pontos.saveAndFlush(ponto);
    }
    private String login(Usuario usuario) throws Exception {
        String response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", usuario.getEmail(), "senha", "Senha@123"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("accessToken").asText();
    }
    private String bearer(Usuario usuario) throws Exception { return "Bearer " + login(usuario); }
    private JsonNode criarRelato(Usuario autor, PontoCritico ponto) throws Exception {
        String response = mvc.perform(post("/api/v1/relatos").header("Authorization", bearer(autor))
            .contentType(MediaType.APPLICATION_JSON).content(relatoJson(ponto)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }
    private String relatoJson(PontoCritico ponto) throws Exception {
        return json.writeValueAsString(Map.of("pontoCriticoId", ponto.getId(), "descricao", "Elevador está sem funcionamento nesta entrada.",
            "fotoUrl", "https://example.test/elevador.jpg", "tipoProblema", "ELEVADOR_QUEBRADO"));
    }
}
