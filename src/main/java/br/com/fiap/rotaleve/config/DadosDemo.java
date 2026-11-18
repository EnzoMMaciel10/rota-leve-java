package br.com.fiap.rotaleve.config;

import br.com.fiap.rotaleve.model.*;
import br.com.fiap.rotaleve.model.Tipos.*;
import br.com.fiap.rotaleve.repository.*;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component @Profile("demo") @RequiredArgsConstructor
public class DadosDemo implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final PerfilMobilidadeRepository perfis;
    private final PontoCriticoRepository pontos;
    private final PasswordEncoder passwords;
    private final Clock clock;
    @Override @Transactional public void run(String... args) {
        Usuario cidadao = usuario("Cidadã de demonstração", "cidadao@rotaleve.local", "Cidadao@123", Papel.CIDADAO);
        usuario("Gestor de demonstração", "gestor@rotaleve.local", "Gestor@123", Papel.GESTOR);
        if (perfis.findByUsuarioId(cidadao.getId()).isEmpty()) {
            var perfil = new PerfilMobilidade(); perfil.setUsuario(cidadao); perfil.setTipo(TipoMobilidade.GESTANTE);
            perfil.setCaminhadaMaximaMetros(600); perfil.setEvitarEscadas(true);
            perfil.setPriorizarElevador(true); perfil.setPriorizarAssento(true); perfis.save(perfil);
        }
        ponto("Elevador Sé (demo)", TipoPonto.ELEVADOR, -23.5505, -46.6333);
        ponto("Elevador República (demo)", TipoPonto.ELEVADOR, -23.5441, -46.6428);
        ponto("Acesso Clínicas (demo)", TipoPonto.RAMPA, -23.5580, -46.6696);
    }
    private Usuario usuario(String nome, String email, String senha, Papel papel) {
        return usuarios.findByEmail(email).orElseGet(() -> {
            var u = new Usuario(); u.setNome(nome); u.setEmail(email); u.setSenhaHash(passwords.encode(senha));
            u.setPapel(papel); return usuarios.save(u);
        });
    }
    private void ponto(String nome, TipoPonto tipo, double lat, double lon) {
        // Reiniciar uma demonstração persistente não apaga relatos nem renova verificação anterior.
        if (pontos.findAll().stream().anyMatch(p -> p.getNome().equals(nome))) return;
        var p = new PontoCritico(); p.setNome(nome); p.setTipo(tipo); p.setLatitude(lat); p.setLongitude(lon);
        p.setEstado(EstadoPonto.OPERACIONAL); p.setUltimaVerificacao(clock.instant()); pontos.save(p);
    }
}
