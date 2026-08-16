package cl.usach.pgt.config;

import cl.usach.pgt.entity.*;
import cl.usach.pgt.entity.enums.*;
import cl.usach.pgt.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final TesisRepository tesisRepository;
    private final HitoEntregaRepository hitoRepository;
    private final EntregaRepository entregaRepository;

    public DataSeeder(UsuarioRepository usuarioRepository,
                      TesisRepository tesisRepository,
                      HitoEntregaRepository hitoRepository,
                      EntregaRepository entregaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.tesisRepository = tesisRepository;
        this.hitoRepository = hitoRepository;
        this.entregaRepository = entregaRepository;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;   // ya hay datos, no duplicar
        }

        Usuario profesor = usuarioRepository.save(new Usuario(
                "11111111-1", "Gonzalo Gonzalez",
                "gonzalo.gonzales@usach.cl", RolUsuario.PROFESOR));

        Usuario estudiante = usuarioRepository.save(new Usuario(
                "22222222-2", "Rodrigo Rodriguez",
                "rodrigo.rodriguez@usach.cl", RolUsuario.ESTUDIANTE));

        Tesis tesis = tesisRepository.save(new Tesis(
                estudiante, profesor,
                "Plataforma de Gestión de Tesistas"));

        // Hito vencido -> sirve para probar la Excepción 1
        hitoRepository.save(new HitoEntrega(
                "Propuesta de Tema", LocalDateTime.now().minusDays(5)));

        // Hito vigente con entrega ya evaluada -> Excepción 3
        HitoEntrega marco = hitoRepository.save(new HitoEntrega(
                "Marco Teórico", LocalDateTime.now().plusDays(15)));
        Entrega evaluada = new Entrega(tesis, marco,
                "marco-teorico.pdf", "documentos-pgt/ejemplo.pdf", 102400);
        evaluada.cambiarEstado(EstadoEntrega.EVALUADO);
        entregaRepository.save(evaluada);

        // Hito vigente y vacío -> camino feliz
        hitoRepository.save(new HitoEntrega(
                "Informe de Avance", LocalDateTime.now().plusDays(30)));

        System.out.println(">>> Datos de prueba cargados. Estudiante id = " + estudiante.getId());
    }
}

