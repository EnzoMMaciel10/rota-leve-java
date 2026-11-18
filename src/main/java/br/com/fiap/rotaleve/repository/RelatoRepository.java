package br.com.fiap.rotaleve.repository;
import br.com.fiap.rotaleve.model.Relato;
import br.com.fiap.rotaleve.model.Tipos.StatusRelato;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
public interface RelatoRepository extends JpaRepository<Relato, Long> {
    @EntityGraph(attributePaths = {"usuario", "pontoCritico"})
    Page<Relato> findAllByStatusAndExpiraEmAfter(StatusRelato status, Instant agora, Pageable pageable);
    @EntityGraph(attributePaths = {"usuario", "pontoCritico"})
    List<Relato> findByStatusAndExpiraEmBefore(StatusRelato status, Instant agora);
    boolean existsByPontoCriticoIdAndStatusAndExpiraEmAfter(Long pontoId, StatusRelato status, Instant agora);
    boolean existsByPontoCriticoId(Long pontoId);
    @EntityGraph(attributePaths = {"usuario", "pontoCritico"})
    @Query("select r from Relato r where :status is null or "
        + "(r.status = :status and (:status <> :ativo or r.expiraEm > :agora)) or "
        + "(:status = :expirado and r.status = :ativo and r.expiraEm <= :agora)")
    Page<Relato> findByFiltro(StatusRelato status, StatusRelato ativo, StatusRelato expirado, Instant agora, Pageable pageable);
    default Page<Relato> findByStatusEfetivo(StatusRelato status, Instant agora, Pageable pageable) {
        return findByFiltro(status, StatusRelato.ATIVO, StatusRelato.EXPIRADO, agora, pageable);
    }
}
