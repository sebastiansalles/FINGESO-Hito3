package cl.usach.pgt.controller;

import cl.usach.pgt.dto.EntregaResponse;
import cl.usach.pgt.dto.PanelHitoResponse;
import cl.usach.pgt.service.EntregaService;
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
    public List<PanelHitoResponse> verPanel(@RequestHeader("X-Usuario-Id") Long usuarioId) {
        return entregaService.verPanel(usuarioId);
    }

    @PostMapping("/entregas")
    public EntregaResponse registrarEntrega(
            @RequestHeader("X-Usuario-Id") Long usuarioId,
            @RequestParam("hitoId") Long hitoId,
            @RequestParam("archivo") MultipartFile archivo) {

        return entregaService.registrarEntrega(usuarioId, hitoId, archivo);
    }
}