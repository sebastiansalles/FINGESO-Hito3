package cl.usach.pgt.service;

import cl.usach.pgt.entity.Entrega;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    /** OP-02 · enviarNotificacion() — RF-09 */
    public void enviarNotificacion(Entrega entrega) {
        String destinatario = entrega.getTesis().getProfesorGuia().getCorreoInstitucional();
        String estudiante = entrega.getTesis().getEstudiante().getNombre();

        log.info("[NOTIFICACIÓN] Para: {} | Asunto: Nueva entrega de {} | Hito: {} | Archivo: {}",
                destinatario, estudiante,
                entrega.getHito().getNombre(), entrega.getNombreArchivo());
    }
}