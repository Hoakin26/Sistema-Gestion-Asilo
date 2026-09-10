package app.cabezadealgodon.com.gestionasilobackend.services;

import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.LoginRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.LoginResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;
import app.cabezadealgodon.com.gestionasilobackend.security.JwtService;

/** HU-30 (YS-47): valida credenciales y emite el token de sesión. */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository,
            JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Los correos se guardan en minúsculas, así que iniciar sesión no puede
        // depender de cómo el usuario escribió las mayúsculas.
        String correo = request.correo().trim().toLowerCase();

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(correo, request.password()));

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con el correo " + correo));

        String token = jwtService.generarToken(usuario.getCorreo(), usuario.getRol().getNombre().name());

        return new LoginResponse(token, "Bearer", usuario.getCorreo(), usuario.getNombreCompleto(),
                usuario.getRol().getNombre());
    }

    /**
     * YS-68: marca el momento del cierre de sesión. Todo token emitido antes
     * deja de aceptarse, incluidos los de otros dispositivos.
     */
    @Transactional
    public void logout(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con el correo " + correo));

        usuario.setFechaInvalidacionSesiones(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }
}
