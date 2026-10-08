package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.util.List;

/**
 * YS-58. La ficha médica completa.
 *
 * <p>
 * Lleva el nombre del interno para que la pantalla pueda encabezar la ficha sin
 * una segunda petición, pero no repite el resto de sus datos personales: para
 * eso está {@link InternoResponse}.
 */
public record FichaMedicaResponse(
        Long id,
        Long internoId,
        String nombreInterno,
        String tipoSangre,
        String alergias,
        String motivoIngreso,
        List<PsicopatologiaResponse> psicopatologias,
        List<MedicamentoHabitualResponse> medicamentosHabituales) {
}
