package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * YS-154. El reporte de análisis médico de un paciente (HU-25, RF-18): sus
 * datos generales, su ficha médica (con el motivo de reclusión, que en la ficha
 * es el motivo de ingreso) y todo su historial: las visitas con sus
 * diagnósticos, los medicamentos recetados y los exámenes. Es el reporte con
 * más información clínica del sistema; solo para los médicos (RNF-03).
 *
 * <p>
 * Reutiliza la ficha de YS-58 y el historial centralizado de HU-02 tal cual,
 * para que el reporte diga exactamente lo mismo que las pantallas.
 */
public record ReporteAnalisisMedicoResponse(
        Paciente paciente,
        FichaMedicaResponse ficha,
        HistorialMedicoResponse historial) {

    /** Los datos generales del paciente; sin la cuota ni el familiar, que no son de este reporte. */
    public record Paciente(Long id, String nombreCompleto, String dpi, LocalDate fechaNacimiento,
            LocalDate fechaIngreso, boolean activo) {
    }
}
