package br.com.fiap.rotaleve.config;

import br.com.fiap.rotaleve.model.Usuario;
import br.com.fiap.rotaleve.model.Tipos.Papel;
import br.com.fiap.rotaleve.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component @Profile("prod") @RequiredArgsConstructor
public class BootstrapGestor implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwords;
    @Value("${BOOTSTRAP_GESTOR_EMAIL:}") private String email;
    @Value("${BOOTSTRAP_GESTOR_SENHA:}") private String senha;
    @Override @Transactional public void run(String... args) {
        if (email.isBlank() && senha.isBlank()) return;
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || senha.length() < 12
            || senha.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Configure e-mail válido e senha de bootstrap com 12 a 72 bytes");
        String normalizado = email.trim().toLowerCase(Locale.ROOT);
        var existente = usuarios.findByEmail(normalizado);
        if (existente.isPresent()) {
            if (existente.get().getPapel() != Papel.GESTOR)
                throw new IllegalStateException("Bootstrap não promove conta de cidadão existente");
            return;
        }
        var gestor = new Usuario(); gestor.setNome("Gestor inicial"); gestor.setEmail(normalizado);
        gestor.setSenhaHash(passwords.encode(senha)); gestor.setPapel(Papel.GESTOR); usuarios.save(gestor);
    }
}
