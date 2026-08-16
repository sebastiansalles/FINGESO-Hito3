package cl.usach.pgt.service;

import cl.usach.pgt.dto.EntregaResponse;
import cl.usach.pgt.dto.PanelHitoResponse;
import cl.usach.pgt.entity.*;
import cl.usach.pgt.entity.enums.EstadoTesis;
import cl.usach.pgt.entity.enums.RolUsuario;
import cl.usach.pgt.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class EntregaService {

    private static final long TAMANO_MAXIMO_BYTES = 20L * 1024 * 1024;   // RNF-04
    private static final List<String> EXTENSIONES_PERMITIDAS = List.of("pdf", "docx");

    private final UsuarioRepository usuarioRepository;
    private final TesisRepository tesisRepository;
    private final HitoEntregaRepository hitoRepository;
    private final EntregaRepository entregaRepository;
    private final ArchivoService archivoService;
    private final NotificacionService notificacionService;

    public EntregaService(UsuarioRepository usuarioRepository,
                          TesisRepository tesisRepository,
                          HitoEntregaRepository hitoRepository,
                          EntregaRepository entregaRepository,
                          ArchivoService archivoService,
                          NotificacionService notificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.tesisRepository = tesisRepository;
        this.hitoRepository = hitoRepository;
        this.entregaRepository = entregaRepository;
        this.archivoService = archivoService;
        this.notificacionService = notificacionService;
    }

    /** Pantalla "Mis Entregas": los hitos con el estado de cada entrega. */
    public List<PanelHitoResponse> verPanel(Long usuarioId) {
        Usuario estudiante = verificarSesionYRol(usuarioId);
        Tesis tesis = buscarTesisActiva(estudiante);

        List<PanelHitoResponse> panel = new ArrayList<>();
        for (HitoEntrega hito : hitoRepository.findAllByOrderByFechaLimiteAsc()) {
            Entrega entrega = entregaRepository
                    .findByTesisIdAndHitoId(tesis.getId(), hito.getId())
                    .orElse(null);

            panel.add(new PanelHitoResponse(
                    hito.getId(),
                    hito.getNombre(),
                    hito.getFechaLimite(),
                    hito.verificarPlazoVigente(),
                    entrega == null ? null : entrega.getNombreArchivo(),
                    entrega == null ? null : entrega.getFechaHoraCarga(),
                    entrega == null ? null : entrega.getEstado().name()
            ));
        }
        return panel;
    }

    /** CU_009 · Registrar Entrega Parcial */
    @Transactional
    public EntregaResponse registrarEntrega(Long usuarioId, Long hitoId, MultipartFile archivo) {

        Usuario estudiante = verificarSesionYRol(usuarioId);              // OP-01
        Tesis tesis = buscarTesisActiva(estudiante);

        HitoEntrega hito = hitoRepository.findById(hitoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El hito indicado no existe."));

        verificarPlazoVigente(hito);                                      // OP-11
        validarArchivo(archivo);                                          // OP-12

        Entrega entrega = entregaRepository
                .findByTesisIdAndHitoId(tesis.getId(), hito.getId())
                .orElse(null);

        if (entrega != null && entrega.estaEvaluada()) {
            throw new ReglaNegocioException(
                    "La entrega de este hito ya fue evaluada y no puede reemplazarse.");
        }

        String ruta = archivoService.guardar(archivo, tesis.getId(), hito.getId());

        entrega = almacenarEntrega(entrega, tesis, hito, archivo, ruta);  // OP-13
        notificacionService.enviarNotificacion(entrega);                  // OP-02

        return new EntregaResponse(
                entrega.getId(),
                hito.getNombre(),
                entrega.getNombreArchivo(),
                entrega.getFechaHoraCarga(),
                entrega.getEstado().name()
        );
    }

    /** OP-01 · verificarSesionYRol() — RF-01 */
    private Usuario verificarSesionYRol(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));

        if (usuario.getRol() != RolUsuario.ESTUDIANTE) {
            throw new ReglaNegocioException("Solo un estudiante puede registrar entregas.");
        }
        return usuario;
    }

    /** OP-11 · verificarPlazoVigente() — RF-06 · Excepción 1 del CU_009 */
    private void verificarPlazoVigente(HitoEntrega hito) {
        if (!hito.verificarPlazoVigente()) {
            throw new ReglaNegocioException(
                    "Plazo vencido: el hito \"" + hito.getNombre() + "\" cerró el "
                            + hito.getFechaLimite() + ".");
        }
    }

    /** OP-12 · validarArchivo() — RNF-04 · Excepción 2 del CU_009 */
    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Debe adjuntar un archivo.");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new ReglaNegocioException("El archivo supera el máximo de 20 MB.");
        }
        String extension = ArchivoService.extensionDe(archivo.getOriginalFilename());
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new ReglaNegocioException("Solo se aceptan archivos PDF o DOCX.");
        }
    }

    /** OP-13 · almacenarEntrega() — RF-07 */
    private Entrega almacenarEntrega(Entrega existente, Tesis tesis, HitoEntrega hito,
                                     MultipartFile archivo, String ruta) {
        if (existente == null) {
            return entregaRepository.save(new Entrega(
                    tesis, hito, archivo.getOriginalFilename(), ruta, archivo.getSize()));
        }
        existente.actualizarDocumento(
                archivo.getOriginalFilename(), ruta, archivo.getSize());
        return entregaRepository.save(existente);
    }

    private Tesis buscarTesisActiva(Usuario estudiante) {
        return tesisRepository
                .findByEstudianteIdAndEstado(estudiante.getId(), EstadoTesis.ACTIVA)
                .orElseThrow(() -> new ReglaNegocioException(
                        "No tiene una tesis activa asignada."));
    }
}