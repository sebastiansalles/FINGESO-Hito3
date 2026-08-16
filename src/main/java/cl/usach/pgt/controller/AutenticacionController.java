package cl.usach.pgt.controller;

import cl.usach.pgt.dto.LoginRequest;
import cl.usach.pgt.dto.UsuarioResponse;
import cl.usach.pgt.entity.Usuario;
import cl.usach.pgt.repository.UsuarioRepository;
import cl.usach.pgt.service.AutenticacionService;
import cl.usach.pgt.service.NoAutenticadoException;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sesion")
public class AutenticacionController {

    private final AutenticacionService autenticacionService;
    private final UsuarioRepository usuarioRepository;

    public AutenticacionController(AutenticacionService autenticacionService,
                                   UsuarioRepository usuarioRepository) {
        this.autenticacionService = autenticacionService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    public UsuarioResponse iniciarSesion(@RequestBody LoginRequest datos, HttpSession sesion) {
        Usuario usuario = autenticacionService.iniciarSesion(datos.rut(), datos.contrasena());
        sesion.setAttribute("usuarioId", usuario.getId());
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getRol().name());
    }

    @GetMapping
    public UsuarioResponse sesionActual(HttpSession sesion) {
        Long id = (Long) sesion.getAttribute("usuarioId");
        if (id == null) {
            throw new NoAutenticadoException("No hay sesión activa.");
        }
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NoAutenticadoException("No hay sesión activa."));
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getRol().name());
    }

    @DeleteMapping
    public void cerrarSesion(HttpSession sesion) {
        sesion.invalidate();
    }
}