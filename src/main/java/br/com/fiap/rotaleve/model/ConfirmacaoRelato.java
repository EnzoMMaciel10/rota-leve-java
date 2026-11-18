package br.com.fiap.rotaleve.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "rl_confirmacao", uniqueConstraints = @UniqueConstraint(columnNames = {"relato_id", "usuario_id"}))
@Getter @Setter @NoArgsConstructor
public class ConfirmacaoRelato {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "usuario_id", nullable = false) private Usuario usuario;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "relato_id", nullable = false) private Relato relato;
}
