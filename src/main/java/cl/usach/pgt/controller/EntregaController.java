package cl.usach.pgt.controller;

import cl.usach.pgt.dto.EntregaResponse;
import cl.usach.pgt.dto.PanelHitoResponse;
import cl.usach.pgt.dto.TesisResponse;
import cl.usach.pgt.service.EntregaService;
import cl.usach.pgt.service.NoAutenticadoException;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class EntregaController {

    private final EntregaService entregaService;

    public EntregaController(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    @GetMapping("/panel")
    public List<PanelHitoResponse> verPanel(HttpSession sesion) {
        return entregaService.verPanel(usuarioAutenticado(sesion));
    }

    @PostMapping("/entregas")
    public EntregaResponse registrarEntrega(
            @RequestParam("hitoId") Long hitoId,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "comentario", required = false) String comentario,
            HttpSession sesion) {

        return entregaService.registrarEntrega(
                usuarioAutenticado(sesion), hitoId, archivo, comentario);
    }

    @GetMapping("/tesis")
    public TesisResponse verTesis(HttpSession sesion) {
        return entregaService.verTesis(usuarioAutenticado(sesion));
    }

    /** OP-01 · La identidad viene de la sesión del servidor, no de la petición. */
    private Long usuarioAutenticado(HttpSession sesion) {
        Long id = (Long) sesion.getAttribute("usuarioId");
        if (id == null) {
            throw new NoAutenticadoException("Debe iniciar sesión para continuar.");
        }
        return id;
    }
}