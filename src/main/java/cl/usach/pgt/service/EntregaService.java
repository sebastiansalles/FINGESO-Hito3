package cl.usach.pgt.service;

import cl.usach.pgt.dto.EntregaResponse;
import cl.usach.pgt.dto.PanelHitoResponse;
import cl.usach.pgt.dto.TesisResponse;
import cl.usach.pgt.entity.*;
import cl.usach.pgt.entity.enums.EstadoTesis;
import cl.usach.pgt.entity.enums.RolUsuario;
import cl.usach.pgt.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class EntregaService {

    private static final long TAMANO_MAXIMO_BYTES = 20 * 1024 * 1024;   // RNF-04
    private static final int LARGO_MAXIMO_COMENTARIO = 500;
    private static final List<String> EXTENSIONES_PERMITIDAS = List.of("pdf", "docx");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy 'a las' HH:mm");

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
                    entrega == null ? null : entrega.getEstado().name(),
                    entrega == null ? null : entrega.getComentario()
            ));
        }
        return panel;
    }

    public TesisResponse verTesis(Long usuarioId) {
        Usuario estudiante = verificarSesionYRol(usuarioId);
        Tesis tesis = buscarTesisActiva(estudiante);

        return new TesisResponse(
                tesis.getTituloTema(),
                tesis.getProfesorGuia().getNombre(),
                tesis.getEstado().name()
        );
    }

    //CU_009 - Registrar Entrega Parcial
    @Transactional
    public EntregaResponse registrarEntrega(Long usuarioId, Long hitoId,
                                            MultipartFile archivo, String comentario) {

        Usuario estudiante = verificarSesionYRol(usuarioId); // OP-01
        Tesis tesis = buscarTesisActiva(estudiante);

        HitoEntrega hito = hitoRepository.findById(hitoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El hito seleccionado no está disponible."));

        verificarPlazoVigente(hito); // OP-11

        Entrega entrega = entregaRepository
                .findByTesisIdAndHitoId(tesis.getId(), hito.getId())
                .orElse(null);

        if (entrega != null && entrega.estaEvaluada()) {
            throw new ReglaNegocioException("Esta entrega ya fue evaluada y no puede modificarse.");
        }

        validarArchivo(archivo);    // OP-12
        validarComentario(comentario);

        String ruta = archivoService.guardar(archivo, tesis, hito);

        entrega = almacenarEntrega(entrega, tesis, hito, archivo, ruta, comentario);  // OP-13
        notificacionService.enviarNotificacion(entrega);                  // OP-02

        return new EntregaResponse(
                entrega.getId(),
                hito.getNombre(),
                entrega.getNombreArchivo(),
                entrega.getFechaHoraCarga(),
                entrega.getEstado().name()
        );
    }




    //OP-01 - verificarSesionYRol()
    private Usuario verificarSesionYRol(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));

        if (usuario.getRol() != RolUsuario.ESTUDIANTE) {
            throw new ReglaNegocioException("Solo un estudiante puede registrar entregas.");
        }
        return usuario;
    }

    //OP-11 - verificarPlazoVigente()
    private void verificarPlazoVigente(HitoEntrega hito) {
        if (!hito.verificarPlazoVigente()) {
            throw new ReglaNegocioException("El plazo del hito \"" + hito.getNombre() + "\" venció el "
                            + hito.getFechaLimite().format(FORMATO_FECHA) + ".");
        }
    }

    //OP-12 - validarArchivo()
    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Debe adjuntar un archivo.");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            String peso = String.format("%.1f", archivo.getSize() / (1024.0 * 1024.0));
            throw new ReglaNegocioException(
                    "El archivo pesa " + peso + " MB. El máximo permitido es 20 MB.");
        }
        String extension = ArchivoService.extensionDe(archivo.getOriginalFilename());
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new ReglaNegocioException("Solo se aceptan archivos PDF o DOCX.");
        }
    }

    private void validarComentario(String comentario) {
        if (comentario != null && comentario.length() > LARGO_MAXIMO_COMENTARIO) {
            throw new ReglaNegocioException(
                    "El comentario no puede superar los 500 caracteres.");
        }
    }

    //OP-13 - almacenarEntrega()
    private Entrega almacenarEntrega(Entrega existente, Tesis tesis, HitoEntrega hito,
                                     MultipartFile archivo, String ruta, String comentario) {
        if (existente == null) {
            return entregaRepository.save(new Entrega(
                    tesis, hito, archivo.getOriginalFilename(),
                    ruta, archivo.getSize(), comentario));
        }
        existente.actualizarDocumento(
                archivo.getOriginalFilename(), ruta, archivo.getSize(), comentario);
        return entregaRepository.save(existente);
    }

    private Tesis buscarTesisActiva(Usuario estudiante) {
        return tesisRepository
                .findByEstudianteIdAndEstado(estudiante.getId(), EstadoTesis.ACTIVA)
                .orElseThrow(() -> new ReglaNegocioException("Sin tesis asignada"));
    }
}