package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDateTime;
import java.util.List;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoReceta;

/**
 * YS-108. Una receta con sus medicamentos. No lleva el diagnóstico de la
 * visita: la farmacia (HU-15) también la verá, y para despachar no lo
 * necesita (mínimo privilegio, sección 5 del diseño).
 */
public record RecetaResponse(
        Long id,
        Long visitaId,
        EstadoReceta estado,
        LocalDateTime fechaEmision,
        String indicacionesGenerales,
        List<Medicamento> medicamentos) {

    public record Medicamento(Long id, String medicamento, String dosis, int cantidad, String formaAplicacion) {
    }
}
