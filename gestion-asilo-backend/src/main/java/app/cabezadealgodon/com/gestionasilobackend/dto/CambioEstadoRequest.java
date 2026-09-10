package app.cabezadealgodon.com.gestionasilobackend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Boolean y no boolean: con el primitivo, un cuerpo sin el campo llegaría como
 * false y desactivaría al usuario en silencio.
 */
public record CambioEstadoRequest(
        @NotNull(message = "Debe indicar si el usuario queda activo o inactivo")
        Boolean activo) {
}
