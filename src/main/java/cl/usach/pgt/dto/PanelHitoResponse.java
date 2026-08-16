package cl.usach.pgt.dto;

import java.time.LocalDateTime;

public record PanelHitoResponse(
        Long hitoId,
        String nombreHito,
        LocalDateTime fechaLimite,
        boolean plazoVigente,
        String nombreArchivo,
        LocalDateTime fechaHoraCarga,
        String estadoEntrega
) {
}