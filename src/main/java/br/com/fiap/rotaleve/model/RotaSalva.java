package br.com.fiap.rotaleve.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "rl_rota_salva") @Getter @Setter @NoArgsConstructor
public class RotaSalva {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(nullable = false) private Usuario usuario;
    @Column(nullable = false, length = 120) private String nome;
    @Column(nullable = false, length = 200) private String origem;
    @Column(nullable = false, length = 200) private String destino;
    @Lob @Column(nullable = false) private String snapshotJson;
    @ManyToMany @JoinTable(name = "rl_rota_ponto", joinColumns = @JoinColumn(name = "rota_id"), inverseJoinColumns = @JoinColumn(name = "ponto_id"))
    private Set<PontoCritico> pontosCriticos = new LinkedHashSet<>();
    @Column(nullable = false) private Instant criadaEm;
}
