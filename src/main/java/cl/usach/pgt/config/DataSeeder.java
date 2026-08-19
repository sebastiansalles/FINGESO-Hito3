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

        // Profesores guía
        // No se usan para iniciar sesión: existen porque toda Tesis requiere
        // un profesor guía y porque son los destinatarios de las
        // notificaciones automáticas
        Usuario profesorGonzalez = usuarioRepository.save(new Usuario(
                "12345678-1", "Gonzalo Gonzalez", "gonzalo.gonzales@usach.cl",
                RolUsuario.PROFESOR, passwordEncoder.encode("123")));

        Usuario profesorRodriguez = usuarioRepository.save(new Usuario(
                "12345678-2", "Rodrigo Rodriguez", "rodrigo.rodriguez@usach.cl",
                RolUsuario.PROFESOR, passwordEncoder.encode("123")));

        // ---------- Estudiantes ----------
        Usuario martin = usuarioRepository.save(new Usuario(
                "12345678-3", "Martin Martinez", "martin.martinez@usach.cl",
                RolUsuario.ESTUDIANTE, passwordEncoder.encode("123")));

        Usuario fernando = usuarioRepository.save(new Usuario(
                "12345678-4", "Fernando Fernandez", "fernando.fernandez@usach.cl",
                RolUsuario.ESTUDIANTE, passwordEncoder.encode("123")));

        // Sin tesis oficializada: aún no ha pasado por el CU_007
        usuarioRepository.save(new Usuario(
                "12345678-5", "Esteban Estévez", "esteban.estevez@usach.cl",
                RolUsuario.ESTUDIANTE, passwordEncoder.encode("123")));

        // ---------- Tesis oficializadas ----------
        Tesis tesisMartin = tesisRepository.save(new Tesis(
                martin, profesorGonzalez,
                "Algoritmos Óptimos en Computación Cuántica"));

        tesisRepository.save(new Tesis(
                fernando, profesorRodriguez,
                "Pérdida de bits debido a los rayos cósmicos"));

        // Calendario de hitos ----------
        // Los plazos cierran a las 23:59 y son relativos a hoy

        HitoEntrega hito1 = hitoRepository.save(new HitoEntrega(
                "Hito 1: Definición del problema",
                LocalDate.now().minusDays(45).atTime(23, 59)));

        hitoRepository.save(new HitoEntrega(
                "Hito 2: Formulación de Hipótesis",
                LocalDate.now().minusDays(10).atTime(23, 59)));

        HitoEntrega hito3 = hitoRepository.save(new HitoEntrega(
                "Hito 3: Desarrollo del proyecto",
                LocalDate.now().plusDays(15).atTime(23, 59)));

        hitoRepository.save(new HitoEntrega(
                "Hito 4: Informe final",
                LocalDate.now().plusDays(40).atTime(23, 59)));

        // Historial de Martin
        // Hito 1: entregado a tiempo y ya evaluado
        Entrega entregaHito1 = new Entrega(tesisMartin, hito1,
                "hito1-definicion-problema.pdf",
                "documentos-pgt/" + martin.getRut() + "/hito-" + hito1.getId() + ".pdf",
                189440,
                "Se define el problema y su contexto");
        entregaHito1.ajustarFechaCarga(LocalDate.now().minusDays(47).atTime(18, 20));
        entregaHito1.cambiarEstado(EstadoEntrega.EVALUADO);
        entregaRepository.save(entregaHito1);

        // Hito 2: no entregó, el plazo ya venció

        // Hito 3: entregado y evaluado, con el plazo todavía abierto
        Entrega entregaHito3 = new Entrega(tesisMartin, hito3,
                "hito3-desarrollo.pdf",
                "documentos-pgt/" + martin.getRut() + "/hito-" + hito3.getId() + ".pdf",
                215040,
                "Correcciones cap 3 y 4. Nuevos cap 5 y 6");
        entregaHito3.ajustarFechaCarga(LocalDate.now().minusDays(3).atTime(21, 5));
        entregaHito3.cambiarEstado(EstadoEntrega.EVALUADO);
        entregaRepository.save(entregaHito3);

        // Hito 4: sin entrega

        imprimirCredenciales();
    }

    private void imprimirCredenciales() {
        System.out.println("""

             DATOS DE PRUEBA:
             
             RUT           CLAVE   ESTUDIANTE
             12345678-3    123     Martín Martínez (con historial de entregas)
             12345678-4    123     Fernando Fernández (sin entregas)
             12345678-5    123     Esteban Estévez (sin tesis asignada)
            """);
    }
}