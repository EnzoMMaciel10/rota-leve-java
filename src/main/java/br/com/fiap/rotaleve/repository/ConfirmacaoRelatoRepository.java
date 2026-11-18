package br.com.fiap.rotaleve.repository;
import br.com.fiap.rotaleve.model.ConfirmacaoRelato;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ConfirmacaoRelatoRepository extends JpaRepository<ConfirmacaoRelato, Long> {
    boolean existsByRelatoIdAndUsuarioId(Long relatoId, Long usuarioId);
    long countByRelatoId(Long relatoId);
    void deleteByRelatoId(Long relatoId);
}
