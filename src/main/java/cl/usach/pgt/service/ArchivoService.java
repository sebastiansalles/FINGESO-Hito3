package cl.usach.pgt.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

@Service
public class ArchivoService {

    private static final Path CARPETA = Paths.get("documentos-pgt");

    /** Guarda el archivo y devuelve la ruta donde quedó. */
    public String guardar(MultipartFile archivo, Long tesisId, Long hitoId) {
        try {
            Files.createDirectories(CARPETA);

            String nombre = "tesis-" + tesisId + "_hito-" + hitoId
                    + "." + extensionDe(archivo.getOriginalFilename());
            Path destino = CARPETA.resolve(nombre);

            Files.copy(archivo.getInputStream(), destino,
                    StandardCopyOption.REPLACE_EXISTING);

            return destino.toString();
        } catch (IOException e) {
            throw new RuntimeException("No fue posible guardar el archivo.", e);
        }
    }

    public static String extensionDe(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            return "";
        }
        return nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1).toLowerCase();
    }
}