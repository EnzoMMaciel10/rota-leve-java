package br.com.fiap.rotaleve.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import br.com.fiap.rotaleve.model.Tipos.*;

@Entity @Table(name = "rl_ponto_critico") @Getter @Setter @NoArgsConstructor
public class PontoCritico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 160) private String nome;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private TipoPonto tipo;
    @Column(nullable = false) private double latitude;
    @Column(nullable = false) private double longitude;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private EstadoPonto estado = EstadoPonto.DESCONHECIDO;
    private Instant ultimaVerificacao;
}
