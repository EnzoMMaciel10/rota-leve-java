package br.com.fiap.rotaleve.routes;

import java.text.Normalizer;
import java.util.List;
import br.com.fiap.rotaleve.repository.PontoCriticoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name="rotaleve.routes.provider", havingValue="demo", matchIfMissing=true)
public class ProvedorDemo implements ProvedorRotas {
    private final PontoCriticoRepository pontos;
    @Override public String nome() { return "DEMO_SINTETICA"; }
    @Override public List<RotasDtos.Candidata> buscar(String origem, String destino) {
        if (!normalizar(origem).equals("praca da se") || !normalizar(destino).equals("hospital das clinicas"))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "O modo demo aceita somente Praça da Sé → Hospital das Clínicas. Ative Google para outras buscas.");
        long se = ponto("Elevador Sé (demo)");
        long republica = ponto("Elevador República (demo)");
        long clinicas = ponto("Acesso Clínicas (demo)");
        return List.of(
            new RotasDtos.Candidata("demo-se", origem, destino, "Metrô pela Sé (cenário simulado)", nome(),
                1680, 300, false, true, true, true, List.of(se, clinicas), null),
            new RotasDtos.Candidata("demo-republica", origem, destino, "Alternativa pela República (cenário simulado)", nome(),
                1980, 400, false, true, true, true, List.of(republica, clinicas), null),
            new RotasDtos.Candidata("demo-escadas", origem, destino, "Opção com escadas e caminhada longa (simulada)", nome(),
                1500, 1100, true, false, false, false, List.of(), null));
    }
    private long ponto(String nome) {
        return pontos.findAll().stream().filter(p -> p.getNome().equals(nome)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Dados demo não inicializados")).getId();
    }
    private String normalizar(String valor) {
        return Normalizer.normalize(valor.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
    }
}
