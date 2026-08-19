package cl.usach.pgt.service;

import cl.usach.pgt.entity.HitoEntrega;
import cl.usach.pgt.entity.Tesis;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

@Service
public class ArchivoService {

    private static final Path RAIZ = Paths.get("documentos-pgt");

    //Guarda el documento en una carpeta por estudiante y devuelve su ruta.
    //El nombre es determinista: reenviar el mismo hito reemplaza el archivo anterior.

    public String guardar(MultipartFile archivo, Tesis tesis, HitoEntrega hito) {
        try {
            Path carpeta = RAIZ.resolve(tesis.getEstudiante().getRut());
            Files.createDirectories(carpeta);

            String base = "hito-" + hito.getId();
            borrarVersionesAnteriores(carpeta, base);

            Path destino = carpeta.resolve(
                    base + "." + extensionDe(archivo.getOriginalFilename()));

            Files.copy(archivo.getInputStream(), destino,
                    StandardCopyOption.REPLACE_EXISTING);

            return destino.toString();
        } catch (IOException e) {
            throw new RuntimeException("No fue posible guardar el archivo.", e);
        }
    }

    //Elimina entregas previas del mismo hito, cualquiera sea su extensión
    private void borrarVersionesAnteriores(Path carpeta, String base) throws IOException {
        try (DirectoryStream<Path> previos = Files.newDirectoryStream(carpeta, base + ".*")) {
            for (Path anterior : previos) {
                Files.delete(anterior);
            }
        }
    }

    public static String extensionDe(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            return "";
        }
        return nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1).toLowerCase();
    }
}