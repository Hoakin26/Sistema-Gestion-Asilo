package app.cabezadealgodon.com.gestionasilobackend.services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.CambioPasswordRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.LoginRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.LoginResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.CambioPasswordInvalidoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;
import app.cabezadealgodon.com.gestionasilobackend.security.JwtService;

/** HU-30: valida credenciales, emite el token de sesión y administra la sesión y la contraseña propias. */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository,
            JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Los correos se guardan en minúsculas, así que iniciar sesión no puede
        // depender de cómo el usuario escribió las mayúsculas.
        String correo = request.correo().trim().toLowerCase();

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(correo, request.password()));

        return respuestaPara(buscar(correo));
    }

    /**
     * YS-68: marca el momento del cierre de sesión. Todo token emitido antes
     * deja de aceptarse, incluidos los de otros dispositivos.
     */
    @Transactional
    public void logout(String correo) {
        Usuario usuario = buscar(correo);
        usuario.setFechaInvalidacionSesiones(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    /**
     * YS-69: cambia la contraseña propia e invalida todas las sesiones abiertas.
     * Devuelve un token nuevo para que quien hizo el cambio siga dentro.
     */
    @Transactional
    public LoginResponse cambiarPassword(String correo, CambioPasswordRequest request) {
        Usuario usuario = buscar(correo);

        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPasswordHash())) {
            throw new CambioPasswordInvalidoException("passwordActual", "La contraseña actual no es correcta");
        }
        if (passwordEncoder.matches(request.passwordNueva(), usuario.getPasswordHash())) {
            throw new CambioPasswordInvalidoException("passwordNueva",
                    "La contraseña nueva debe ser distinta de la actual");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
        // Truncada a milisegundos: PostgreSQL guarda microsegundos y, al
        // redondear, la marca podría pasar al milisegundo siguiente y dejar al
        // token de abajo con fecha anterior a ella, rechazado al usarse.
        usuario.setFechaInvalidacionSesiones(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        usuarioRepository.save(usuario);

        return respuestaPara(usuario);
    }

    private Usuario buscar(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con el correo " + correo));
    }

    private LoginResponse respuestaPara(Usuario usuario) {
        String token = jwtService.generarToken(usuario.getCorreo(), usuario.getRol().getNombre().name());
        return new LoginResponse(token, "Bearer", usuario.getCorreo(), usuario.getNombreCompleto(),
                usuario.getRol().getNombre());
    }
}
