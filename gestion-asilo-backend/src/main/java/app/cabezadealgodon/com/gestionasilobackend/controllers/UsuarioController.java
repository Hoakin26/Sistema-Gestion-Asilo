package app.cabezadealgodon.com.gestionasilobackend.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import app.cabezadealgodon.com.gestionasilobackend.dto.CambioEstadoRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.RestablecerPasswordRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.UsuarioRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.UsuarioResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.UsuarioUpdateRequest;
import app.cabezadealgodon.com.gestionasilobackend.security.UsuarioAutenticado;
import app.cabezadealgodon.com.gestionasilobackend.services.UsuarioService;
import jakarta.validation.Valid;

/** HU-29 (YS-41). La restricción por rol se agrega en YS-52/YS-53 (HU-31). */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest request) {
        return usuarioService.crear(request);
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest request) {
        return usuarioService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest request) {
        return usuarioService.cambiarEstado(id, request.activo());
    }

    /**
     * YS-71. Recibe quién lo pide para impedir que alguien se restablezca su
     * propia contraseña por esta vía, que no exige la anterior.
     */
    @PutMapping("/{id}/password")
    public UsuarioResponse restablecerPassword(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado solicitante,
            @Valid @RequestBody RestablecerPasswordRequest request) {
        return usuarioService.restablecerPassword(id, solicitante.getUsername(), request);
    }
}
