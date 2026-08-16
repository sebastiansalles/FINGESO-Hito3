package cl.usach.pgt.repository;

import cl.usach.pgt.entity.HitoEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HitoEntregaRepository extends JpaRepository<HitoEntrega, Long> {
    List<HitoEntrega> findAllByOrderByFechaLimiteAsc();
}