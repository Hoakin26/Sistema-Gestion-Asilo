package app.cabezadealgodon.com.gestionasilobackend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.LoginRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.LoginResponse;
import app.cabezadealgodon.com.gestionasilobackend.security.UsuarioAutenticado;
import app.cabezadealgodon.com.gestionasilobackend.services.AuthService;
import jakarta.validation.Valid;

/** HU-30 (YS-47). Ruta pública: sin ella nadie podría obtener un token. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** YS-68. Requiere token válido: se cierra la sesión de quien la tiene abierta. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        authService.logout(usuario.getUsername());
    }
}
