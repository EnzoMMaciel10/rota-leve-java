package br.com.fiap.rotaleve.messaging;

import java.time.Instant;

/** Contrato versionado do evento interno; não carrega dados pessoais. */
public record EventoPonto(String eventId, Long pontoId, String mensagem, Instant ocorridoEm) {
    public EventoPonto {
        if (eventId == null || eventId.isBlank() || pontoId == null || pontoId <= 0
                || mensagem == null || mensagem.isBlank() || ocorridoEm == null) {
            throw new IllegalArgumentException("Evento de ponto incompleto");
        }
    }
}
