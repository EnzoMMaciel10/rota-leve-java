package br.com.fiap.rotaleve.routes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.retry.support.RetryTemplate;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.assertThat;

/** Exercita Spring AI real contra um servidor HTTP local que simula o protocolo do provedor. */
class SpringAiIntegracaoTests {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final AtomicReference<String> corpoRecebido = new AtomicReference<>();
    private final AtomicInteger chamadas = new AtomicInteger();
    private final AtomicReference<String> segundaResposta = new AtomicReference<>();
    private HttpServer server;
    private int status = 200;
    private String resposta = """
            {"id":"chatcmpl-teste","object":"chat.completion","created":1794830400,
            "model":"modelo-local-teste","choices":[{"index":0,"message":{"role":"assistant",
            "content":"A rota prevê 400 metros a pé, 30 minutos de viagem no total e nota de adequação de 82 pontos."},"finish_reason":"stop"}],
            "usage":{"prompt_tokens":25,"completion_tokens":20,"total_tokens":45}}
            """;

    @BeforeEach
    void iniciarServidorLocal() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            corpoRecebido.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            int chamada = chamadas.incrementAndGet();
            String corpo = chamada > 1 && segundaResposta.get() != null ? segundaResposta.get() : resposta;
            byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void encerrarServidorLocal() { if (server != null) server.stop(0); }

    @Test
    void chatClientRealExplicaRotaComApenasMetricasAnonimas() throws Exception {
        var resposta = service(true, true).explicar(rota());

        assertThat(resposta.fonte()).isEqualTo("SPRING_AI");
        assertThat(resposta.texto()).contains("Cenário sintético", "Dados verificados:", "400 m");
        assertThat(resposta.textoGerado()).contains("400 metros", "30 minutos", "82 pontos");
        assertThat(chamadas.get()).isEqualTo(1);
        assertThat(corpoRecebido.get()).doesNotContain("Rua Particular", "Clínica Pré-natal", "gestante", "cadastro@privado");
        JsonNode payload = mapper.readTree(corpoRecebido.get());
        assertThat(payload.path("model").asText()).isEqualTo("modelo-local-teste");
        String user = null;
        for (JsonNode mensagem : payload.path("messages")) {
            if ("user".equals(mensagem.path("role").asText())) user = mensagem.path("content").asText();
        }
        assertThat(user).isNotBlank();
        var dados = mapper.readTree(user);
        var campos = new HashSet<String>();
        dados.fieldNames().forEachRemaining(campos::add);
        assertThat(campos).containsExactlyInAnyOrder("caminhadaMetros", "duracaoMinutos", "score", "compatibilidade", "demonstracao");
        assertThat(dados.path("caminhadaMetros").asInt()).isEqualTo(400);
        assertThat(dados.path("demonstracao").asBoolean()).isTrue();
        assertThat(dados.path("compatibilidade").asText()).isEqualTo("Compatível apenas com os dados disponíveis.");
    }

    @Test
    void erroHttpDoModeloPreservaAvaliacaoEmFallbackExplicito() {
        status = 400;
        resposta = "{\"error\":{\"message\":\"falha simulada\",\"type\":\"invalid_request_error\",\"code\":\"invalid_request_error\"}}";
        var resultado = service(true, true).explicar(rota());
        assertThat(resultado.fonte()).isEqualTo("FALLBACK_LLM_INDISPONIVEL");
        assertThat(resultado.texto()).contains("400 m", "82/100").doesNotContain("falha simulada");
        assertThat(chamadas.get()).isEqualTo(1);
        assertThat(resultado.textoGerado()).isNull();
    }

    @Test
    void demonstracaoDesabilitadaNaoChamaModelo() {
        var resultado = service(false, true).explicar(rota());
        assertThat(resultado.fonte()).isEqualTo("REGRAS_LOCAIS_SEM_LLM");
        assertThat(chamadas.get()).isZero();
        assertThat(resultado.textoGerado()).isNull();
    }

    @Test
    void ausenciaDeBeanDoModeloEhInformadaComoFallback() {
        var resultado = service(true, false).explicar(rota());
        assertThat(resultado.fonte()).isEqualTo("FALLBACK_LLM_INDISPONIVEL");
        assertThat(chamadas.get()).isZero();
        assertThat(resultado.textoGerado()).isNull();
    }

    @Test
    void rejeitaGarantiaInventadaDepoisDeDuasTentativasSemExporTexto() throws Exception {
        resposta = respostaComTexto("Esta rota garante uma viagem confortável e segura de 400 metros.");
        var resultado = service(true, true).explicar(rota());
        assertThat(resultado.fonte()).isEqualTo("FALLBACK_LLM_RESPOSTA_INVALIDA");
        assertThat(resultado.textoGerado()).isNull();
        assertThat(resultado.texto()).doesNotContain("confortável", "segura de 400");
        assertThat(chamadas.get()).isEqualTo(2);
        assertThat(corpoRecebido.get()).doesNotContain("confortável", "gestante", "Rua Particular");
    }

    @Test
    void rejeitaCompatibilidadeInventadaQuandoFaltamDados() throws Exception {
        resposta = respostaComTexto("O perfil é compatível com os dados disponíveis e a caminhada prevista é de 400 metros.");
        var original = rota();
        var avaliacao = new RotasDtos.Avaliacao(original.avaliacao().rota(), 82,
            RotasDtos.Compatibilidade.DADOS_INSUFICIENTES, List.of("Dados insuficientes"), original.criadaEm());
        var desconhecida = new RotasDtos.SalvaResponse(original.id(), original.nome(), original.origem(), original.destino(), original.criadaEm(), avaliacao);
        var resultado = service(true, true).explicar(desconhecida);
        assertThat(resultado.fonte()).isEqualTo("FALLBACK_LLM_RESPOSTA_INVALIDA");
        assertThat(resultado.textoGerado()).isNull();
        assertThat(resultado.texto()).contains("Faltam informações; não é possível confirmar a acessibilidade.")
            .doesNotContain("O perfil é compatível");
        assertThat(chamadas.get()).isEqualTo(2);
    }

    @Test
    void segundaGeracaoValidaPodeSerExibidaSemIncluirRascunhoRejeitado() throws Exception {
        String valida = resposta;
        resposta = respostaComTexto("Esta rota garante segurança de 400 metros.");
        segundaResposta.set(valida);
        var resultado = service(true, true).explicar(rota());
        assertThat(resultado.fonte()).isEqualTo("SPRING_AI");
        assertThat(resultado.textoGerado()).contains("400 metros", "30 minutos", "82 pontos");
        assertThat(resultado.texto()).doesNotContain("garante segurança");
        assertThat(chamadas.get()).isEqualTo(2);
        assertThat(corpoRecebido.get()).doesNotContain("garante segurança");
    }

    private String respostaComTexto(String texto) throws Exception {
        var json = mapper.readTree(resposta);
        ((com.fasterxml.jackson.databind.node.ObjectNode) json.path("choices").get(0).path("message")).put("content", texto);
        return mapper.writeValueAsString(json);
    }

    private ExplicacaoService service(boolean enabled, boolean modeloDisponivel) {
        var factory = new DefaultListableBeanFactory();
        if (modeloDisponivel) {
            var api = OpenAiApi.builder().apiKey("token-ficticio-apenas-teste-local")
                    .baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build();
            var modelo = OpenAiChatModel.builder().openAiApi(api)
                    .defaultOptions(OpenAiChatOptions.builder().model("modelo-local-teste").build())
                    .retryTemplate(RetryTemplate.builder().maxAttempts(1).build()).build();
            factory.registerSingleton("chatBuilderTeste", ChatClient.builder(modelo));
        }
        return new ExplicacaoService(factory.getBeanProvider(ChatClient.Builder.class), mapper, enabled);
    }

    private RotasDtos.SalvaResponse rota() {
        var agora = Instant.parse("2026-11-16T12:00:00Z");
        var candidata = new RotasDtos.Candidata("rota-1", "Rua Particular, 999", "Clínica Pré-natal",
                "gestante cadastro@privado.local", "DEMO_SINTETICA", 1800, 400,
                false, true, true, true, List.of(), null);
        var avaliacao = new RotasDtos.Avaliacao(candidata, 82,
                RotasDtos.Compatibilidade.COMPATIVEL_COM_DADOS_CONHECIDOS,
                List.of("Dados sintéticos", "Informação pessoal da gestante"), agora);
        return new RotasDtos.SalvaResponse(7L, "Rota privada", candidata.origem(), candidata.destino(), agora, avaliacao);
    }
}
