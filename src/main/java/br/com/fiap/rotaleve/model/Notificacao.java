package br.com.fiap.rotaleve.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "rl_notificacao", uniqueConstraints = @UniqueConstraint(columnNames = {"evento_id", "rota_salva_id"}))
@Getter @Setter @NoArgsConstructor
public class Notificacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(nullable = false) private Usuario usuario;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "rota_salva_id", nullable = false) private RotaSalva rotaSalva;
    @Column(name = "evento_id", nullable = false, length = 50) private String eventoId;
    @Column(nullable = false, length = 1000) private String mensagem;
    @Column(nullable = false) private boolean lida;
    @Column(nullable = false) private Instant criadaEm;
}
