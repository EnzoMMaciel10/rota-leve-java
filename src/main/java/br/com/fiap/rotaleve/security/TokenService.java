package br.com.fiap.rotaleve.security;

import br.com.fiap.rotaleve.dto.CoreDtos.*;
import br.com.fiap.rotaleve.model.Usuario;
import br.com.fiap.rotaleve.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class TokenService {
    @Value("${rotaleve.jwt.expiration-minutes:120}") private long expirationMinutes;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest dto) {
        if (expirationMinutes < 1 || expirationMinutes > 1440) throw new IllegalStateException("Validade do JWT deve ser entre 1 e 1440 minutos.");
        long ttl = Duration.ofMinutes(expirationMinutes).getSeconds();
        Usuario usuario = usuarios.findByEmail(dto.email().strip().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos."));
        if (!encoder.matches(dto.senha(), usuario.getSenhaHash())) throw new BadCredentialsException("E-mail ou senha inválidos.");
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("rota-leve").subject(usuario.getId().toString())
            .issuedAt(clock.instant()).expiresAt(clock.instant().plusSeconds(ttl)).claim("papel", usuario.getPapel().name()).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", ttl, new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPapel()));
    }
}
