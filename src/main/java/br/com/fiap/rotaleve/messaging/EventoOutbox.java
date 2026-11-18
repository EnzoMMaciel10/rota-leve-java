package br.com.fiap.rotaleve.messaging;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

/** Mantém o evento no mesmo commit da alteração do relato. */
@Entity
@Table(name = "rl_evento_outbox", indexes = @Index(name = "ix_outbox_pendente", columnList = "enviado,proxima_tentativa_em"))
@Getter @Setter @NoArgsConstructor
public class EventoOutbox {
    @Id @Column(length = 36)
    private String id;
    @Column(nullable = false)
    private Long pontoId;
    @Column(nullable = false, length = 1000)
    private String mensagem;
    @Column(nullable = false)
    private Instant criadoEm;
    @Column(name = "proxima_tentativa_em", nullable = false)
    private Instant proximaTentativaEm;
    @Column(nullable = false)
    private boolean enviado;
    @Column(nullable = false)
    private int tentativas;
    @Column(length = 120)
    private String ultimaFalha;
    private Instant enviadoEm;

    public EventoPonto evento() {
        return new EventoPonto(id, pontoId, mensagem, criadoEm);
    }
}
