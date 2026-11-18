package br.com.fiap.rotaleve.routes;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;

class ValidadorTextoRotaTest {
    /** Regressões capturadas do Qwen real em 17/11; integração bem sucedida não as torna corretas. */
    static Stream<Arguments> geracoesIncorretasObservadas() {
        return Stream.of(
            Arguments.of("A viagem planejada prevê uma caminhada de 300 metros, totalizando 30 minutos. "
                + "Esta estimativa é compatível com os dados disponíveis, garantindo uma viagem agradável e confortável. "
                + "O score de 90 indica uma adequação ótima ao perfil, destacando a facilidade de acesso e a segurança do trajeto.",
                300, 90, RotasDtos.Compatibilidade.COMPATIVEL_COM_DADOS_CONHECIDOS),
            Arguments.of("A viagem planejada prevê uma caminhada de 1100 metros, totalizando 30 minutos incluindo transporte e espera. "
                + "Esta opção é incompatível com o perfil, pois não atende aos requisitos de acessibilidade e segurança. "
                + "Para uma viagem adequada, seria necessário buscar uma alternativa que ofereça condições de caminhada compatíveis com os dados disponíveis.",
                1100, 0, RotasDtos.Compatibilidade.INCOMPATIVEL),
            Arguments.of("A viagem planejada prevê uma caminhada de 350 metros, totalizando 30 minutos incluindo transporte e espera. "
                + "O perfil é compatível com os dados disponíveis, mas a nota de adequação é de 55, indicando uma boa compatibilidade.",
                350, 55, RotasDtos.Compatibilidade.DADOS_INSUFICIENTES));
    }

    @ParameterizedTest @MethodSource("geracoesIncorretasObservadas")
    void rejeitaRegressoesReaisDoModelo(String gerado, int metros, int score, RotasDtos.Compatibilidade estado) {
        assertThat(ValidadorTextoRota.valido(gerado, avaliacao(metros, score, estado))).isFalse();
    }

    @Test
    void aceitaResumoPlanejadoComUnidadesENotaCorretas() {
        assertThat(ValidadorTextoRota.valido("A rota prevê 300 metros a pé, 30 minutos de viagem no total e nota de adequação de 90 pontos.",
            avaliacao(300, 90, RotasDtos.Compatibilidade.COMPATIVEL_COM_DADOS_CONHECIDOS))).isTrue();
    }

    @Test
    void rejeitaNumeroInventadoETrocaDeUnidadesMesmoUsandoNumerosDoJson() {
        var dados = avaliacao(300, 90, RotasDtos.Compatibilidade.COMPATIVEL_COM_DADOS_CONHECIDOS);
        assertThat(ValidadorTextoRota.valido("A rota prevê 500 metros a pé.", dados)).isFalse();
        assertThat(ValidadorTextoRota.valido("A rota prevê 30 metros a pé e duração total de 300 minutos.", dados)).isFalse();
        assertThat(ValidadorTextoRota.valido("A rota prevê 300 metros e nota de adequação de 30 pontos.", dados)).isFalse();
    }

    @Test
    void rejeitaConversaoVelocidadeEPassadoRealizado() {
        var dados = avaliacao(1100, 0, RotasDtos.Compatibilidade.INCOMPATIVEL);
        assertThat(ValidadorTextoRota.valido("A pessoa caminhou 1.1 km em 30 minutos, com velocidade média de 4 km/h.", dados)).isFalse();
        assertThat(ValidadorTextoRota.valido("A viagem tem duração total de 30 minutos e garante segurança.", dados)).isFalse();
    }

    @Test
    void naoAfirmaCompatibilidadeQuandoFaltamDados() {
        assertThat(ValidadorTextoRota.valido("A rota prevê 350 metros e o perfil é compatível com os dados disponíveis.",
            avaliacao(350, 55, RotasDtos.Compatibilidade.DADOS_INSUFICIENTES))).isFalse();
    }

    @Test
    void rejeitaVazioSemNumerosELongoDemais() {
        var dados = avaliacao(300, 90, RotasDtos.Compatibilidade.COMPATIVEL_COM_DADOS_CONHECIDOS);
        assertThat(ValidadorTextoRota.valido(null, dados)).isFalse();
        assertThat(ValidadorTextoRota.valido(" ", dados)).isFalse();
        assertThat(ValidadorTextoRota.valido("As informações do plano foram informadas.", dados)).isFalse();
        assertThat(ValidadorTextoRota.valido("300 metros " + "planejamento ".repeat(81), dados)).isFalse();
    }

    @Test
    void numerosCoincidentesNaoQuebramValidacao() {
        assertThat(ValidadorTextoRota.valido("A rota prevê 100 metros a pé, 30 minutos de viagem no total e nota de adequação de 100 pontos.",
            avaliacao(100, 100, RotasDtos.Compatibilidade.COMPATIVEL_COM_DADOS_CONHECIDOS))).isTrue();
    }

    private static RotasDtos.Avaliacao avaliacao(int metros, int score, RotasDtos.Compatibilidade estado) {
        var rota = new RotasDtos.Candidata("fixture", "Origem sintética", "Destino sintético", "Avaliação",
            "DEMO_SINTETICA", 1800, metros, null, null, null, null, List.of(), null);
        return new RotasDtos.Avaliacao(rota, score, estado, List.of(), Instant.parse("2026-11-17T12:00:00Z"));
    }
}
