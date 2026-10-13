package app.cabezadealgodon.com.gestionasilobackend.services;

import java.util.List;

import app.cabezadealgodon.com.gestionasilobackend.dto.RecetaResponse;
import app.cabezadealgodon.com.gestionasilobackend.models.Receta;

/**
 * YS-108. La receta como DTO. Vive aparte porque la arman dos servicios: la
 * visita, que la registra, y el historial, que la muestra entre los
 * antecedentes (HU-08). Si cada uno la mapeara por su cuenta, los dos
 * terminarían mostrando cosas distintas de la misma receta.
 */
final class MapeoReceta {

    private MapeoReceta() {
    }

    static RecetaResponse receta(Receta receta) {
        return new RecetaResponse(
                receta.getId(),
                receta.getVisita().getId(),
                receta.getEstado(),
                receta.getFechaEmision(),
                receta.getIndicacionesGenerales(),
                medicamentos(receta));
    }

    static List<RecetaResponse.Medicamento> medicamentos(Receta receta) {
        return receta.getMedicamentos().stream()
                .map(m -> new RecetaResponse.Medicamento(m.getId(), m.getMedicamento(), m.getDosis(),
                        m.getCantidad(), m.getFormaAplicacion()))
                .toList();
    }
}
