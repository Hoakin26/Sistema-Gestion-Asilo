package app.cabezadealgodon.com.gestionasilobackend.dto;

/** YS-58. El familiar responsable tal como lo devuelve la API. */
public record FamiliarResponse(
        Long id,
        String nombreCompleto,
        String parentesco,
        String telefono,
        String correo,
        String direccion) {
}
