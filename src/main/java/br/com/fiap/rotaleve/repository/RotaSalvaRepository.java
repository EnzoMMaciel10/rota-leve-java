package br.com.fiap.rotaleve.repository;
import br.com.fiap.rotaleve.model.RotaSalva;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RotaSalvaRepository extends JpaRepository<RotaSalva, Long> {
    Page<RotaSalva> findByUsuarioId(Long usuarioId, Pageable pageable);
    @EntityGraph(attributePaths = {"usuario"})
    List<RotaSalva> findAllByPontosCriticosId(Long pontoId);
    boolean existsByPontosCriticosId(Long pontoId);
}
