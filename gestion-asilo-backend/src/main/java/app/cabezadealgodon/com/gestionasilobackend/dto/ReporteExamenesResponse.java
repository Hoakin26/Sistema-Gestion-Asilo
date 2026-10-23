package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import app.cabezadealgodon.com.gestionasilobackend.dto.ReporteAnalisisMedicoResponse.Paciente;

/**
 * YS-163. El reporte de exámenes de un paciente (HU-28, RF-21a): los que el
 * laboratorio le realizó entre dos fechas, con el examen, la fecha y el
 * resultado. "Realizado" es con el resultado cargado (HU-13): las órdenes
 * pendientes, en proceso o anuladas no están, y la fecha que filtra es la del
 * resultado. Datos clínicos: solo para los médicos (RNF-03).
 */
public record ReporteExamenesResponse(
        Paciente paciente,
        LocalDate desde,
        LocalDate hasta,
        List<ExamenRealizado> examenes) {

    /**
     * Un examen realizado: cuándo se ordenó y cuándo salió el resultado, el
     * resultado con su archivo, si tiene, y quién lo pidió.
     */
    public record ExamenRealizado(
            Long id,
            String examen,
            LocalDateTime fechaOrden,
            LocalDateTime fechaResultado,
            String resultado,
            ArchivoResultadoResponse archivo,
            EspecialidadResponse especialidad,
            ResumenPersona especialista) {
    }
}
