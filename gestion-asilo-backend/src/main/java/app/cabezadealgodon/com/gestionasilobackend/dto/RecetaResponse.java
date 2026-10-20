package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDateTime;
import java.util.List;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoReceta;

/**
 * YS-108. Una receta con sus medicamentos. No lleva el diagnóstico de la
 * visita: la farmacia (HU-15) también la verá, y para despachar no lo
 * necesita (mínimo privilegio, sección 5 del diseño).
 *
 * <p>
 * YS-123. Con el paciente y el médico que la emitió, que es lo que la bandeja
 * de la farmacia muestra de cada receta (HU-15). Los dos van como
 * {@link ResumenPersona}, sin el DPI, igual que en la bandeja del laboratorio.
 *
 * <p>
 * YS-126. Con lo que registra el despacho (HU-16): la fecha del último y, en
 * cada medicamento, si ya se entregó y cuándo. Antes del primer despacho van
 * vacíos y sin entregar.
 */
public record RecetaResponse(
        Long id,
        Long visitaId,
        ResumenPersona paciente,
        ResumenPersona medico,
        EstadoReceta estado,
        LocalDateTime fechaEmision,
        LocalDateTime fechaDespacho,
        String indicacionesGenerales,
        List<Medicamento> medicamentos) {

    public record Medicamento(Long id, String medicamento, String dosis, int cantidad, String formaAplicacion,
            boolean entregado, LocalDateTime fechaEntrega) {
    }
}
