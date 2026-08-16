package cl.usach.pgt.service;

import cl.usach.pgt.entity.Usuario;
import cl.usach.pgt.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AutenticacionService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AutenticacionService(UsuarioRepository usuarioRepository,
                                PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** RF-01 · Inicio de sesión */
    public Usuario iniciarSesion(String rut, String contrasena) {
        Usuario usuario = usuarioRepository.findByRut(rut)
                .orElseThrow(() -> new ReglaNegocioException("RUT o contraseña incorrectos."));

        if (!passwordEncoder.matches(contrasena, usuario.getContrasena())) {
            throw new ReglaNegocioException("RUT o contraseña incorrectos.");
        }
        return usuario;
    }
}