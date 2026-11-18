package br.com.fiap.rotaleve.repository;
import br.com.fiap.rotaleve.model.PerfilMobilidade;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PerfilMobilidadeRepository extends JpaRepository<PerfilMobilidade, Long> {
    Optional<PerfilMobilidade> findByUsuarioId(Long usuarioId);
    void deleteByUsuarioId(Long usuarioId);
}
