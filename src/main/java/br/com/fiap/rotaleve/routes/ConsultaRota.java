package br.com.fiap.rotaleve.routes;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
public class ConsultaRota {
    @Id private UUID id;
    @Column(nullable=false) private Long usuarioId;
    @Lob @Column(nullable=false) private String candidatasJson;
    @Column(nullable=false) private Instant expiraEm;
}
