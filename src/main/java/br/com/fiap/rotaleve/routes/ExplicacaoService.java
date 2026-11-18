package br.com.fiap.rotaleve.routes;

import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

/** O domínio decide a compatibilidade; o LLM resume somente as métricas planejadas. */
@Service
public class ExplicacaoService {
    private static final String PROMPT = "Escreva exatamente uma frase em português sobre uma rota planejada. "
        + "Informe apenas a caminhada prevista em metros, a duração TOTAL da viagem em minutos "
        + "e a nota de adequação em pontos. Seja literal, sem opiniões. "
        + "Não interprete a nota nem conclua se a rota é adequada. "
        + "Use esta estrutura: A rota prevê ... metros a pé, ... minutos de viagem no total "
        + "e nota de adequação de ... pontos.";
    private static final String PROMPT_CORRECAO = "Responda somente com uma frase numérica em português. "
        + "Use exatamente os valores do JSON nesta estrutura: A rota prevê [caminhadaMetros] metros a pé, "
        + "[duracaoMinutos] minutos de viagem no total e nota de adequação de [score] pontos. "
        + "Não acrescente condições, opiniões ou conclusão. Ignore compatibilidade e demonstracao no resumo; "
        + "o sistema informa esses campos separadamente.";
    private final ObjectProvider<ChatClient.Builder> builders;
    private final ObjectMapper mapper;
    private final boolean habilitada;

    public ExplicacaoService(ObjectProvider<ChatClient.Builder> builders, ObjectMapper mapper,
                            @Value("${rotaleve.ai.enabled:false}") boolean habilitada) {
        this.builders = builders; this.mapper = mapper; this.habilitada = habilitada;
    }

    public RotasDtos.ExplicacaoResponse explicar(RotasDtos.SalvaResponse salva) {
        var a = salva.avaliacao();
        String base = "Caminhada: " + a.rota().caminhadaMetros() + " m. Duração estimada total da viagem: "
            + (a.rota().duracaoSegundos()/60) + " min. Score: " + a.score() + "/100. Avaliação: "
            + a.compatibilidade() + ". " + String.join(". ", a.motivos()) + ".";
        String aviso = "Consulte as condições no local. A avaliação depende dos dados disponíveis e não garante acessibilidade.";
        String situacao = switch (a.compatibilidade()) {
            case COMPATIVEL_COM_DADOS_CONHECIDOS -> "Compatível apenas com os dados disponíveis.";
            case INCOMPATIVEL -> "Esta opção não atende ao perfil; é preciso buscar outra alternativa.";
            case DADOS_INSUFICIENTES -> "Faltam informações; não é possível confirmar a acessibilidade.";
        };
        String prefixo = ("DEMO_SINTETICA".equals(a.rota().fonte())
                ? "Cenário sintético de demonstração; não representa condições reais. " : "")
            + situacao + " A avaliação não garante acessibilidade.";
        if (!habilitada) return resposta(salva, prefixo, base, aviso, "REGRAS_LOCAIS_SEM_LLM", null);
        boolean houveRespostaInvalida = false;
        try {
            var builder = builders.getIfAvailable();
            if (builder == null) throw new IllegalStateException("Modelo não configurado");
            // Apenas cinco fatos agregados; nenhum endereço, perfil de saúde ou texto comunitário.
            String dados = mapper.writeValueAsString(Map.of("caminhadaMetros", a.rota().caminhadaMetros(),
                "duracaoMinutos", a.rota().duracaoSegundos()/60, "score", a.score(),
                "compatibilidade", situacao, "demonstracao", "DEMO_SINTETICA".equals(a.rota().fonte())));
            for (int tentativa = 0; tentativa < 2; tentativa++) {
                String texto = builder.build().prompt().system(tentativa == 0 ? PROMPT : PROMPT_CORRECAO)
                    .user(dados).call().content();
                if (ValidadorTextoRota.valido(texto, a)) {
                    return resposta(salva, prefixo, base, aviso, "SPRING_AI", texto.strip());
                }
                houveRespostaInvalida = true;
            }
            return resposta(salva, prefixo, base, aviso, "FALLBACK_LLM_RESPOSTA_INVALIDA", null);
        } catch (Exception ex) {
            return resposta(salva, prefixo, base, aviso,
                houveRespostaInvalida ? "FALLBACK_LLM_RESPOSTA_INVALIDA" : "FALLBACK_LLM_INDISPONIVEL", null);
        }
    }

    private RotasDtos.ExplicacaoResponse resposta(RotasDtos.SalvaResponse salva, String prefixo,
            String base, String aviso, String fonte, String gerado) {
        String texto = prefixo + (gerado == null ? "" : "\n\n" + gerado) + "\n\nDados verificados: " + base;
        return new RotasDtos.ExplicacaoResponse(salva.id(), texto, fonte, salva.avaliacao(), aviso, gerado);
    }
}
