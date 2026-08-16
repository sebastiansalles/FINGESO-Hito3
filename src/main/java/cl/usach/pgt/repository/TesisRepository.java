package cl.usach.pgt.repository;

import cl.usach.pgt.entity.Tesis;
import cl.usach.pgt.entity.enums.EstadoTesis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TesisRepository extends JpaRepository<Tesis, Long> {
    Optional<Tesis> findByEstudianteIdAndEstado(Long estudianteId, EstadoTesis estado);
}