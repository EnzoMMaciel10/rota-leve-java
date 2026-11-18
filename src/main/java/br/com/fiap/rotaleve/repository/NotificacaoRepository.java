package br.com.fiap.rotaleve.repository;
import br.com.fiap.rotaleve.model.Notificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    Page<Notificacao> findByUsuarioId(Long usuarioId, Pageable pageable);
    boolean existsByEventoIdAndRotaSalvaId(String eventoId, Long rotaId);
    void deleteByRotaSalvaId(Long rotaId);
}
