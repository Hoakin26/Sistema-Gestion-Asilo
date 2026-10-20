package app.cabezadealgodon.com.gestionasilobackend.services;

import java.util.List;

import app.cabezadealgodon.com.gestionasilobackend.dto.RecetaResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ResumenPersona;
import app.cabezadealgodon.com.gestionasilobackend.models.Receta;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.models.VisitaMedica;

/**
 * YS-108. La receta como DTO. Vive aparte porque la arman dos servicios: la
 * visita, que la registra, y el historial, que la muestra entre los
 * antecedentes (HU-08). Si cada uno la mapeara por su cuenta, los dos
 * terminarían mostrando cosas distintas de la misma receta. YS-123: la
 * farmacia (HU-15) es el tercero.
 */
final class MapeoReceta {

    private MapeoReceta() {
    }

    static RecetaResponse receta(Receta receta) {
        VisitaMedica visita = receta.getVisita();
        Usuario medico = visita.getEspecialista().getUsuario();
        return new RecetaResponse(
                receta.getId(),
                visita.getId(),
                new ResumenPersona(visita.getInterno().getId(), visita.getInterno().getNombreCompleto()),
                new ResumenPersona(medico.getId(), medico.getNombreCompleto()),
                receta.getEstado(),
                receta.getFechaEmision(),
                receta.getFechaDespacho(),
                receta.getIndicacionesGenerales(),
                medicamentos(receta));
    }

    static List<RecetaResponse.Medicamento> medicamentos(Receta receta) {
        return receta.getMedicamentos().stream()
                .map(m -> new RecetaResponse.Medicamento(m.getId(), m.getMedicamento(), m.getDosis(),
                        m.getCantidad(), m.getFormaAplicacion(), m.isEntregado(), m.getFechaEntrega()))
                .toList();
    }
}
