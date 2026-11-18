package br.com.fiap.rotaleve.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import br.com.fiap.rotaleve.model.Tipos.*;

@Entity @Table(name = "rl_relato") @Getter @Setter @NoArgsConstructor
public class Relato {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(nullable = false) private Usuario usuario;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(nullable = false) private PontoCritico pontoCritico;
    @Column(nullable = false, length = 2000) private String descricao;
    @Column(nullable = false, length = 1000) private String fotoUrl;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private TipoProblema tipoProblema;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatusRelato status = StatusRelato.ATIVO;
    @Column(nullable = false) private Instant criadoEm;
    @Column(nullable = false) private Instant ultimaConfirmacao;
    @Column(nullable = false) private Instant expiraEm;
    @Version private Long version;
}
