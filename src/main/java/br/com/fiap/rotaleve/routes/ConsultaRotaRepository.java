package br.com.fiap.rotaleve.routes;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultaRotaRepository extends JpaRepository<ConsultaRota, UUID> {
    long deleteByExpiraEmBefore(Instant instante);
}
