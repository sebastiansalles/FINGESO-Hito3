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
        Usuario profesorHidalgo = usuarioRepository.save(new Usuario(
                "11111111-1", "Felipe Hidalgo", "felipe.hidalgo@usach.cl",
                RolUsuario.PROFESOR, passwordEncoder.encode("profesor123")));

        Usuario profesorGulppi = usuarioRepository.save(new Usuario(
                "12121212-1", "Enzo Gulppi", "enzo.gulppi@usach.cl",
                RolUsuario.PROFESOR, passwordEncoder.encode("profesor123")));

        // ---------- Coordinación ----------
        usuarioRepository.save(new Usuario(
                "33333333-3", "Mario Nova", "mario.nova@usach.cl",
                RolUsuario.COORDINADOR, passwordEncoder.encode("coordinador123")));

        // ---------- Estudiantes ----------
        Usuario tesistaSalles = usuarioRepository.save(new Usuario(
                "22222222-2", "Sebastian Salles", "sebastian.salles@usach.cl",
                RolUsuario.ESTUDIANTE, passwordEncoder.encode("estudiante123")));

        Usuario tesistaSilva = usuarioRepository.save(new Usuario(
                "23232323-2", "Lucas Silva", "lucas.silva@usach.cl",
                RolUsuario.ESTUDIANTE, passwordEncoder.encode("estudiante123")));

        // Estudiante sin tesis oficializada: aún no ha pasado por el CU_007
        usuarioRepository.save(new Usuario(
                "24242424-2", "Ignacio Caro", "ignacio.caro@usach.cl",
                RolUsuario.ESTUDIANTE, passwordEncoder.encode("estudiante123")));

        // ---------- Tesis oficializadas ----------
        Tesis tesisSalles = tesisRepository.save(new Tesis(
                tesistaSalles, profesorHidalgo,
                "Monitoreo de calidad del aire mediante sensores IoT"));

        tesisRepository.save(new Tesis(
                tesistaSilva, profesorGulppi,
                "Detección de fallas en redes eléctricas con aprendizaje automático"));

        // ---------- Hitos de entrega ----------
        // Los plazos cierran a las 23:59, como en un calendario académico.
        // Las fechas son relativas a hoy para que los escenarios sigan
        // siendo válidos cualquier día que se ejecute la aplicación.

        // Plazo vencido -> Excepción 1
        hitoRepository.save(new HitoEntrega(
                "Hito 1: Hallazgo del problema de investigación",
                LocalDate.now().minusDays(7).atTime(23, 59)));

        // Plazo vigente -> queda bloqueado solo para quien ya fue evaluado
        HitoEntrega hito2 = hitoRepository.save(new HitoEntrega(
                "Hito 2: Desarrollo del proyecto",
                LocalDate.now().plusDays(10).atTime(23, 59)));

        // Plazo vigente y sin entregas -> camino feliz y reenvío
        hitoRepository.save(new HitoEntrega(
                "Hito 3: Etapa final",
                LocalDate.now().plusDays(25).atTime(23, 59)));

        // ---------- Entrega ya evaluada (solo la tesis de Salles) ----------
        Entrega evaluada = new Entrega(tesisSalles, hito2,
                "hito2-desarrollo.pdf",
                "documentos-pgt/" + tesistaSalles.getRut() + "/hito-" + hito2.getId() + ".pdf",
                215040);
        evaluada.cambiarEstado(EstadoEntrega.EVALUADO);
        entregaRepository.save(evaluada);

        imprimirCredenciales();
    }

    private void imprimirCredenciales() {
        System.out.println("""

            ===========================================================
             DATOS DE PRUEBA CARGADOS
            ===========================================================
             RUT           CONTRASEÑA       PERFIL
             -----------------------------------------------------
             22222222-2    estudiante123    Tesista con entrega evaluada
             23232323-2    estudiante123    Tesista sin entregas
             24242424-2    estudiante123    Estudiante sin tesis activa
             11111111-1    profesor123      Profesor guía (Salles)
             12121212-1    profesor123      Profesor guía (Silva)
             33333333-3    coordinador123   Coordinador de Tesis
            ===========================================================
            """);
    }
}