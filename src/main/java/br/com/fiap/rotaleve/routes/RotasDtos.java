package br.com.fiap.rotaleve.routes;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class RotasDtos {
    private RotasDtos() {}
    public enum Compatibilidade { COMPATIVEL_COM_DADOS_CONHECIDOS, DADOS_INSUFICIENTES, INCOMPATIVEL }
    public record BuscaRequest(@NotBlank @Size(max=180) String origem,
                               @NotBlank @Size(max=180) String destino) {}
    public record SalvarRequest(@NotNull java.util.UUID consultaId, @NotBlank String rotaId,
                                 @NotBlank @Size(max=100) String nome) {}
    public record NomeRequest(@NotBlank @Size(max=100) String nome) {}
    // Boolean nulo significa dado desconhecido, nunca ausência confirmada de uma barreira.
    public record Candidata(String id, String origem, String destino, String resumo, String fonte,
                            int duracaoSegundos, int caminhadaMetros, Boolean temEscadas,
                            Boolean embarqueAcessivel, Boolean temAssento, Boolean temCobertura,
                            List<Long> pontosCriticos, String polyline) {}
    public record Avaliacao(Candidata rota, int score, Compatibilidade compatibilidade,
                            List<String> motivos, Instant avaliadaEm) {}
    public record BuscaResponse(java.util.UUID consultaId, Instant expiraEm, String provedor,
                                 boolean demonstracao, boolean temOpcaoCompativel, List<Avaliacao> rotas) {}
    public record SalvaResponse(Long id, String nome, String origem, String destino,
                                Instant criadaEm, Avaliacao avaliacao) {}
    public record ExplicacaoResponse(Long rotaId, String texto, String fonte,
                                     Avaliacao dadosVerificados, String aviso, String textoGerado) {}
}
