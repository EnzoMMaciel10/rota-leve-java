package br.com.fiap.rotaleve.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Opt-in: exercita ChatClient e o modelo Ollama real, sem mock do provedor.
 * OLLAMA_INTEGRATION_TEST=true; requer modelo previamente baixado no Ollama.
 * As métricas são fixtures sintéticas, independentes dos pontos da demonstração.
 * O relatório guarda saídas para revisão humana, sem tokens, usuários ou endereços.
 */
@SpringBootTest(properties = {
        "spring.ai.model.chat=ollama", "rotaleve.ai.enabled=true", "rotaleve.rabbit.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:avaliacao-ollama-real;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.ollama.chat.options.temperature=0.0", "spring.ai.ollama.chat.options.seed=42",
        "spring.ai.ollama.chat.options.num-predict=160", "spring.ai.ollama.chat.options.keep-alive=5m",
        "spring.ai.retry.max-attempts=1", "spring.http.client.connect-timeout=5s",
        "spring.http.client.read-timeout=90s"
})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "OLLAMA_INTEGRATION_TEST", matches = "(?i)true")
class RealOllamaIntegrationTests {
    private static final List<Map<String, Object>> RESULTADOS = new ArrayList<>();
    @Autowired ExplicacaoService explicacoes;

    enum Cenario { COMPATIVEL, BLOQUEADA, INSUFICIENTE }

    @ParameterizedTest(name = "Modelo real: {0}")
    @EnumSource(Cenario.class)
    @Timeout(100)
    void explicaFixturesPreservandoDadosESemConfundirFallbackComGeracao(Cenario cenario) {
        var fixture = fixture(cenario);
        long inicio = System.nanoTime();
        var resposta = explicacoes.explicar(fixture);
        double segundos = (System.nanoTime() - inicio) / 1_000_000_000.0;
        String textoLlm = resposta.textoGerado();
        var resultado = new LinkedHashMap<String, Object>();
        resultado.put("cenario", cenario.name());
        resultado.put("latencia_segundos", Math.round(segundos * 1000) / 1000.0);
        resultado.put("fonte", resposta.fonte());
        resultado.put("texto_llm", textoLlm);
        resultado.put("texto_final", resposta.texto());
        resultado.put("aviso", resposta.aviso());
        resultado.put("dados_verificados", resposta.dadosVerificados());
        resultado.put("qualidade_textual", "PENDENTE_REVISAO_HUMANA");
        resultado.put("criterios_revisao", List.of(
                "Português claro e coerente com as métricas",
                "Sem inventar condições de equipamentos ou garantir acessibilidade",
                "BLOQUEADA: não recomendar a opção incompatível",
                "INSUFICIENTE: não tratar falta de dados como acessibilidade comprovada",
                "Identificar cenário sintético quando informado ao modelo"));
        RESULTADOS.add(resultado);

        assertThat(resposta.fonte()).as("Uma resposta de fallback não comprova geração pelo modelo")
                .isEqualTo("SPRING_AI");
        assertThat(textoLlm).as("O campo separado deve conter uma geração real validada")
                .isNotBlank();
        assertThat(ValidadorTextoRota.valido(textoLlm, fixture.avaliacao())).isTrue();
        assertThat(resposta.texto()).contains("Cenário sintético", "não garante acessibilidade");
        if (cenario == Cenario.BLOQUEADA) assertThat(resposta.texto()).contains("não atende ao perfil");
        if (cenario == Cenario.INSUFICIENTE) assertThat(resposta.texto()).contains("não é possível confirmar a acessibilidade");
        assertThat(resposta.dadosVerificados()).isEqualTo(fixture.avaliacao());
        assertThat(resposta.aviso()).as("Aviso estruturado deve acompanhar qualquer texto gerado").isNotBlank();
        assertThat(resposta.rotaId()).isEqualTo(fixture.id());
    }

    @AfterAll
    static void registrarResultadosSemCredenciais() throws Exception {
        var relatorio = new LinkedHashMap<String, Object>();
        relatorio.put("executado_em_utc", Instant.now().toString());
        relatorio.put("tipo", "SPRING_AI_OLLAMA_REAL_FIXTURES_SINTETICAS");
        relatorio.put("modelo", System.getenv().getOrDefault("OLLAMA_MODEL", "qwen2.5:1.5b"));
        relatorio.put("configuracao", Map.of("temperature", 0.0, "seed", 42, "num_predict", 160));
        relatorio.put("cenarios", RESULTADOS);
        relatorio.put("avaliacao_qualitativa", "Asserções validam integração e dados; os textos brutos exigem revisão humana.");
        Path arquivo = Path.of(System.getProperty("rotaleve.ollama.evaluation.output", "target/avaliacao-ollama-real.json"));
        if (arquivo.getParent() != null) Files.createDirectories(arquivo.getParent());
        new ObjectMapper().findAndRegisterModules().writerWithDefaultPrettyPrinter().writeValue(arquivo.toFile(), relatorio);
    }

    private RotasDtos.SalvaResponse fixture(Cenario cenario) {
        var agora = Instant.parse("2026-11-17T12:00:00Z");
        int caminhada = switch (cenario) { case COMPATIVEL -> 300; case BLOQUEADA -> 1100; case INSUFICIENTE -> 350; };
        int score = switch (cenario) { case COMPATIVEL -> 90; case BLOQUEADA -> 0; case INSUFICIENTE -> 55; };
        var compatibilidade = switch (cenario) {
            case COMPATIVEL -> RotasDtos.Compatibilidade.COMPATIVEL_COM_DADOS_CONHECIDOS;
            case BLOQUEADA -> RotasDtos.Compatibilidade.INCOMPATIVEL;
            case INSUFICIENTE -> RotasDtos.Compatibilidade.DADOS_INSUFICIENTES;
        };
        List<String> motivos = switch (cenario) {
            case COMPATIVEL -> List.of("Compatível com as condições conhecidas na fixture sintética");
            case BLOQUEADA -> List.of("Escadas incompatíveis com o perfil", "Caminhada excede limite do perfil");
            case INSUFICIENTE -> List.of("Escadas e embarque não verificados", "Dados insuficientes para recomendar acessibilidade");
        };
        var candidata = new RotasDtos.Candidata("fixture-" + cenario.name(), "Origem sintética", "Destino sintético",
                "Fixture de avaliação sem ponto real associado", "DEMO_SINTETICA", 1800, caminhada,
                cenario == Cenario.INSUFICIENTE ? null : cenario == Cenario.BLOQUEADA,
                cenario == Cenario.INSUFICIENTE ? null : cenario != Cenario.BLOQUEADA,
                null, null, List.of(), null);
        var avaliacao = new RotasDtos.Avaliacao(candidata, score, compatibilidade, motivos, agora);
        return new RotasDtos.SalvaResponse(1L + cenario.ordinal(), "Fixture sintética " + cenario.name(),
                candidata.origem(), candidata.destino(), agora, avaliacao);
    }
}
