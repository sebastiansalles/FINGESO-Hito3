package cl.usach.pgt.dto;

import java.time.LocalDateTime;

public record EntregaResponse(
        Long id,
        String nombreHito,
        String nombreArchivo,
        LocalDateTime fechaHoraCarga,
        String estado
) {
}