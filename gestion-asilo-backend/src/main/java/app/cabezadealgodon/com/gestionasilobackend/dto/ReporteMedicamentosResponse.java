package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteAnalisisMedicoResponse.Paciente;

/**
 * YS-163. El reporte de medicamentos de un paciente (HU-28, RF-21b): los que la
 * farmacia le entregó entre dos fechas, con el medicamento, la dosis, la
 * cantidad y la fecha de entrega. Lo recetado y todavía sin entregar no está.
 * Datos clínicos: solo para los médicos (RNF-03).
 */
public record ReporteMedicamentosResponse(
        Paciente paciente,
        LocalDate desde,
        LocalDate hasta,
        List<MedicamentoEntregado> medicamentos) {

    /** Un medicamento entregado: qué, cuánto y cómo, cuándo se entregó, y la receta de la que salió. */
    public record MedicamentoEntregado(
            Long id,
            String medicamento,
            String dosis,
            int cantidad,
            String formaAplicacion,
            LocalDateTime fechaEntrega,
            LocalDateTime fechaReceta,
            EspecialidadResponse especialidad,
            ResumenPersona especialista) {
    }
}
