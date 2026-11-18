package br.com.fiap.rotaleve.repository;
import br.com.fiap.rotaleve.model.PontoCritico;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
public interface PontoCriticoRepository extends JpaRepository<PontoCritico, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from PontoCritico p where p.id = :id")
    Optional<PontoCritico> findByIdForUpdate(Long id);
}
