package app.cabezadealgodon.com.gestionasilobackend.dto;

import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;

/** El rol viaja aquí para que el frontend arme su menú (YS-55). */
public record LoginResponse(
        String token,
        String tipo,
        String correo,
        String nombreCompleto,
        RolNombre rol,

        /**
         * YS-74. Avisa que esta sesión no puede usar el sistema hasta cambiar
         * la contraseña. Viaja en el login para que el frontend lleve al
         * usuario directo a esa pantalla, en vez de dejarlo descubrirlo
         * chocando contra un 403 en el primer módulo que abra.
         */
        boolean debeCambiarPassword) {
}
