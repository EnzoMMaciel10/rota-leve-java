package br.com.fiap.rotaleve.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import br.com.fiap.rotaleve.model.Tipos.Papel;

@Entity @Table(name = "rl_usuario") @Getter @Setter @NoArgsConstructor
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String nome;
    @Column(nullable = false, unique = true, length = 180) private String email;
    @Column(nullable = false, length = 100) private String senhaHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Papel papel;
}
