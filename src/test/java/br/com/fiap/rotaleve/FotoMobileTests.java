package br.com.fiap.rotaleve;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:fotomobile;MODE=Oracle;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop", "rotaleve.rabbit.enabled=false",
    "rotaleve.ai.enabled=false", "spring.ai.model.chat=none"
})
@ActiveProfiles("demo") @AutoConfigureMockMvc
class FotoMobileTests {
    static final Path PASTA;
    static { try { PASTA = Files.createTempDirectory("rotaleve-fotos-test-"); } catch (Exception e) { throw new ExceptionInInitializerError(e); } }
    @DynamicPropertySource static void fotos(DynamicPropertyRegistry properties) {
        properties.add("rotaleve.fotos.diretorio", PASTA::toString);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @AfterAll static void limpar() throws Exception {
        try (var arquivos = Files.list(PASTA)) { for (Path arquivo : arquivos.toList()) Files.delete(arquivo); }
        Files.delete(PASTA);
    }
    @Test void exigeLoginParaUpload() throws Exception {
        mvc.perform(multipart("/api/v1/arquivos/fotos").file(png())).andExpect(status().isUnauthorized());
    }
    @Test void fotoValidaGeraUrlPublicaNomeAleatorioEBytesVerificados() throws Exception {
        String body = mvc.perform(multipart("/api/v1/arquivos/fotos").file(png()).header("Authorization", token()))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String url = json.readTree(body).get("url").asText();
        assertThat(url).matches("http://localhost/api/v1/arquivos/fotos/[0-9a-f-]{36}\\.png");
        var response = mvc.perform(get(java.net.URI.create(url).getPath()))
            .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andReturn().getResponse();
        assertThat(ImageIO.read(new java.io.ByteArrayInputStream(response.getContentAsByteArray())).getWidth()).isEqualTo(2);
    }
    @Test void rejeitaConteudoDisfarcadoDeFoto() throws Exception {
        var falso = new MockMultipartFile("arquivo", "foto.png", "image/png", "<script>alert(1)</script>".getBytes());
        mvc.perform(multipart("/api/v1/arquivos/fotos").file(falso).header("Authorization", token()))
            .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.detail").exists());
    }
    @Test void rejeitaFotoVaziaGrandeOuCampoAusente() throws Exception {
        String token = token();
        mvc.perform(multipart("/api/v1/arquivos/fotos").file(new MockMultipartFile("arquivo", new byte[0])).header("Authorization", token))
            .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/arquivos/fotos").file(new MockMultipartFile("arquivo", new byte[5 * 1024 * 1024 + 1])).header("Authorization", token))
            .andExpect(status().isPayloadTooLarge());
        mvc.perform(multipart("/api/v1/arquivos/fotos").header("Authorization", token)).andExpect(status().isBadRequest());
    }
    @Test void naoExpoeNomesArbitrariosDoDisco() throws Exception {
        mvc.perform(get("/api/v1/arquivos/fotos/application.yml")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/arquivos/fotos/00000000-0000-0000-0000-000000000000.png")).andExpect(status().isNotFound());
    }
    @Test void corsPermiteExpoSomenteNasOrigensConfiguradas() throws Exception {
        mvc.perform(options("/api/v1/auth/login").header("Origin", "http://localhost:8081")
            .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "content-type"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
        mvc.perform(options("/api/v1/auth/login").header("Origin", "https://origem-nao-configurada.example")
            .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
    private MockMultipartFile png() throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return new MockMultipartFile("arquivo", "../../nome-original.png", "image/png", bytes.toByteArray());
    }
    private String token() throws Exception {
        String result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", "cidadao@rotaleve.local", "senha", "Cidadao@123"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(result).get("accessToken").asText();
    }
}
