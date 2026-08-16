package cl.usach.pgt.config;

import cl.usach.pgt.entity.*;
import cl.usach.pgt.entity.enums.*;
import cl.usach.pgt.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final TesisRepository tesisRepository;
    private final HitoEntregaRepository hitoRepository;
    private final EntregaRepository entregaRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UsuarioRepository usuarioRepository,
                      TesisRepository tesisRepository,
                      HitoEntregaRepository hitoRepository,
                      EntregaRepository entregaRepository,
                      PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.tesisRepository = tesisRepository;
        this.hitoRepository = hitoRepository;
        this.entregaRepository = entregaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        // ---------- Profesores guía ----------
        // No se usan para iniciar sesión: existen porque toda Tesis requiere
        // un profesor guía y porque son los destinatarios de las
        // notificaciones automáticas (RF-09).
        Usuario profesoraMendez = usuarioRepository.save(new Usuario(
                "10100001-1", "Carolina Méndez", "carolina.mendez@universidad.cl",
                RolUsuario.PROFESOR, passwordEncoder.encode("profesor123")));

        Usuario profesorFuenzalida = usuarioRepository.save(new Usuario(
                "10100002-2", "Rodrigo Fuenzalida", "rodrigo.fuenzalida@universidad.cl",
                RolUsuario.PROFESOR, passwordEncoder.encode("profesor123")));

        // ---------- Estudiantes ----------
        Usuario javiera = usuarioRepository.save(new Usuario(
                "20200001-1", "Javiera Ruiz", "javiera.ruiz@universidad.cl",
                RolUsuario.ESTUDIANTE, passwordEncoder.encode("estudiante123")));

        Usuario matias = usuarioRepository.save(new Usuario(
                "20200002-2", "Matías Contreras", "matias.contreras@universidad.cl",
                RolUsuario.ESTUDIANTE, passwordEncoder.encode("estudiante123")));

        // Sin tesis oficializada: aún no ha pasado por el CU_007
        usuarioRepository.save(new Usuario(
                "20200003-3", "Antonia Vergara", "antonia.vergara@universidad.cl",
                RolUsuario.ESTUDIANTE, passwordEncoder.encode("estudiante123")));

        // ---------- Tesis oficializadas ----------
        Tesis tesisJaviera = tesisRepository.save(new Tesis(
                javiera, profesoraMendez,
                "Monitoreo de calidad del aire mediante sensores IoT"));

        tesisRepository.save(new Tesis(
                matias, profesorFuenzalida,
                "Detección de fallas en redes eléctricas con aprendizaje automático"));

        // ---------- Calendario de hitos ----------
        // Los plazos cierran a las 23:59 y son relativos a hoy, para que
        // los escenarios sigan siendo válidos cualquier día que se ejecute.

        HitoEntrega hito1 = hitoRepository.save(new HitoEntrega(
                "Hito 1: Definición del problema",
                LocalDate.now().minusDays(45).atTime(23, 59)));

        hitoRepository.save(new HitoEntrega(
                "Hito 2: Marco teórico",
                LocalDate.now().minusDays(10).atTime(23, 59)));

        HitoEntrega hito3 = hitoRepository.save(new HitoEntrega(
                "Hito 3: Desarrollo del proyecto",
                LocalDate.now().plusDays(15).atTime(23, 59)));

        hitoRepository.save(new HitoEntrega(
                "Hito 4: Informe final",
                LocalDate.now().plusDays(40).atTime(23, 59)));

        // ---------- Historial de Javiera ----------
        // Hito 1: entregado a tiempo y ya evaluado
        Entrega entregaHito1 = new Entrega(tesisJaviera, hito1,
                "hito1-definicion-problema.pdf",
                "documentos-pgt/" + javiera.getRut() + "/hito-" + hito1.getId() + ".pdf",
                189440,
                "Se acotó el alcance según lo conversado en la primera reunión.");
        entregaHito1.ajustarFechaCarga(LocalDate.now().minusDays(47).atTime(18, 20));
        entregaHito1.cambiarEstado(EstadoEntrega.EVALUADO);
        entregaRepository.save(entregaHito1);

        // Hito 2: no entregó, el plazo ya venció

        // Hito 3: entregado y evaluado, con el plazo todavía abierto
        Entrega entregaHito3 = new Entrega(tesisJaviera, hito3,
                "hito3-desarrollo.pdf",
                "documentos-pgt/" + javiera.getRut() + "/hito-" + hito3.getId() + ".pdf",
                215040,
                "Se incorporaron las correcciones del capítulo de arquitectura.");
        entregaHito3.ajustarFechaCarga(LocalDate.now().minusDays(3).atTime(21, 5));
        entregaHito3.cambiarEstado(EstadoEntrega.EVALUADO);
        entregaRepository.save(entregaHito3);

        // Hito 4: sin entrega -> camino feliz de la demostración

        imprimirCredenciales();
    }

    private void imprimirCredenciales() {
        System.out.println("""

            =====================================================
             DATOS DE PRUEBA CARGADOS
            =====================================================
             RUT           CONTRASENA       ESTUDIANTE
             ----------------------------------------------------
             20200001-1    estudiante123    Javiera Ruiz
                                            (con historial de entregas)
             20200002-2    estudiante123    Matias Contreras
                                            (sin entregas)
             20200003-3    estudiante123    Antonia Vergara
                                            (sin tesis asignada)
            =====================================================
            """);
    }
}