package br.com.fiap.rotaleve.dto;

import br.com.fiap.rotaleve.model.Tipos.*;
import jakarta.validation.constraints.*;
import java.time.Instant;

/** DTOs delimitam os campos aceitos e impedem a exposição do hash de senha. */
public final class CoreDtos {
    private CoreDtos() { }
    public record CadastroRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Email @Size(max = 180) String email,
        @NotBlank @Size(min = 8, max = 72) String senha) { }
    public record LoginRequest(@NotBlank @Email @Size(max = 180) String email, @NotBlank @Size(max = 72) String senha) { }
    public record UsuarioRequest(@NotBlank @Size(max = 120) String nome, @NotBlank @Email @Size(max = 180) String email) { }
    public record UsuarioResponse(Long id, String nome, String email, Papel papel) { }
    public record LoginResponse(String accessToken, String tokenType, long expiresIn, UsuarioResponse usuario) { }
    public record PerfilRequest(
        @NotNull TipoMobilidade tipo,
        @NotNull @Min(50) @Max(10000) Integer caminhadaMaximaMetros,
        @NotNull Boolean evitarEscadas, @NotNull Boolean priorizarElevador, @NotNull Boolean priorizarAssento) { }
    public record PerfilResponse(Long id, TipoMobilidade tipo, Integer caminhadaMaximaMetros,
        boolean evitarEscadas, boolean priorizarElevador, boolean priorizarAssento) { }
    public record PontoRequest(
        @NotBlank @Size(max = 160) String nome, @NotNull TipoPonto tipo,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotNull EstadoPonto estado) { }
    public record PontoResponse(Long id, String nome, TipoPonto tipo, double latitude, double longitude,
        EstadoPonto estado, Instant ultimaVerificacao) { }
    public record RelatoRequest(
        @NotNull @Positive Long pontoCriticoId,
        @NotBlank @Size(min = 10, max = 2000) String descricao,
        @NotBlank @Size(max = 1000) @Pattern(regexp = "https?://[^\\s]+", message = "deve ser uma URL HTTP ou HTTPS válida") String fotoUrl,
        @NotNull TipoProblema tipoProblema) { }
    public record RelatoResponse(Long id, Long usuarioId, Long pontoCriticoId, String descricao, String fotoUrl,
        TipoProblema tipoProblema, StatusRelato status, Instant criadoEm, Instant ultimaConfirmacao, Instant expiraEm, long confirmacoes) { }
    public record ConfirmacaoResponse(Long relatoId, boolean jaConfirmado, Instant expiraEm, long confirmacoes) { }
}
