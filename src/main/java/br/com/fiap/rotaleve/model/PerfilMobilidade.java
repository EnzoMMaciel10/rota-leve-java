package br.com.fiap.rotaleve.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import br.com.fiap.rotaleve.model.Tipos.TipoMobilidade;

@Entity @Table(name = "rl_perfil_mobilidade") @Getter @Setter @NoArgsConstructor
public class PerfilMobilidade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(nullable = false, unique = true) private Usuario usuario;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private TipoMobilidade tipo;
    @Column(nullable = false) private Integer caminhadaMaximaMetros;
    @Column(nullable = false) private boolean evitarEscadas;
    @Column(nullable = false) private boolean priorizarElevador;
    @Column(nullable = false) private boolean priorizarAssento;
}
