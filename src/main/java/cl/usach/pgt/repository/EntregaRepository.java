package cl.usach.pgt.repository;

import cl.usach.pgt.entity.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {
    Optional<Entrega> findByTesisIdAndHitoId(Long tesisId, Long hitoId);
}