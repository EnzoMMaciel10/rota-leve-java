package br.com.fiap.rotaleve.security;

import br.com.fiap.rotaleve.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean UrlBasedCorsConfigurationSource corsConfigurationSource(
        @Value("${rotaleve.cors.origins}") String origins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", cors);
        return source;
    }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecretKey jwtKey(@Value("${rotaleve.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalStateException("rotaleve.jwt.secret deve conter pelo menos 32 bytes.");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
    @Bean JwtDecoder jwtDecoder(SecretKey key, Clock clock, UsuarioRepository usuarios) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamp = new JwtTimestampValidator(java.time.Duration.ofSeconds(30));
        timestamp.setClock(clock);
        OAuth2TokenValidator<Jwt> usuarioExistente = jwt -> {
            try {
                boolean valido = usuarios.findById(Long.valueOf(jwt.getSubject()))
                    .map(u -> u.getPapel().name().equals(jwt.getClaimAsString("papel"))).orElse(false);
                if (valido) return OAuth2TokenValidatorResult.success();
            } catch (NumberFormatException ignored) { }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Usuário ou papel inválido.", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamp, new JwtIssuerValidator("rota-leve"), usuarioExistente));
        return decoder;
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("papel"); authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter(); converter.setJwtGrantedAuthoritiesConverter(authorities);
        http.cors(cors -> {}).csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/v1/arquivos/fotos/*").permitAll()
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/cadastro", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health", "/actuator/health/**", "/error").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((request, response, error) -> {
                    response.setStatus(401); response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    response.setHeader("WWW-Authenticate", "Bearer");
                    mapper.writeValue(response.getOutputStream(), ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Token ausente, inválido ou expirado."));
                }))
            .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, error) -> {
                response.setStatus(403); response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                mapper.writeValue(response.getOutputStream(), ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Seu papel não permite esta operação."));
            }));
        return http.build();
    }
}
