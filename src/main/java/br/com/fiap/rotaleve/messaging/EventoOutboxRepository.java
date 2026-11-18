package br.com.fiap.rotaleve.messaging;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EventoOutboxRepository extends JpaRepository<EventoOutbox, String> {
    List<EventoOutbox> findTop50ByEnviadoFalseAndProximaTentativaEmLessThanEqualOrderByCriadoEmAsc(Instant agora);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EventoOutbox e where e.id = :id")
    Optional<EventoOutbox> bloquear(@Param("id") String id);
}
