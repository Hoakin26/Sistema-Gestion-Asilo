package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * YS-151. El reporte de costos por paciente y cita (HU-24, RF-17): cada cita
 * con lo que costó su consulta, sus exámenes y sus medicamentos, agrupadas por
 * paciente, con los totales y el promedio por cita de cada uno y del reporte.
 *
 * <p>
 * <b>Por tipo de servicio y nunca por nombre</b> (RNF-03): RF-17 pide
 * desglosar exámenes y medicamentos, y el nombre de un medicamento es un dato
 * clínico. Cada cita dice cuántos exámenes y cuántos medicamentos tuvo y cuánto
 * costaron, sin decir cuáles.
 *
 * <p>
 * {@code paciente} es el filtro pedido; vacío, el reporte es de todos.
 */
public record ReporteCostosResponse(
        LocalDate desde,
        LocalDate hasta,
        ResumenPersona paciente,
        List<PacienteCostos> pacientes,
        int cantidadCitas,
        Costos totales,
        BigDecimal promedioPorCita) {

    /** Las citas de un paciente en el rango, de la más antigua a la más reciente, con sus totales. */
    public record PacienteCostos(ResumenPersona paciente, List<CitaCostos> citas, Costos totales,
            BigDecimal promedioPorCita) {
    }

    /** Una cita: su fecha, su número de solicitud y lo que costó. */
    public record CitaCostos(Long visitaId, String numeroSolicitud, LocalDate fecha, Costos costos) {
    }

    /**
     * Lo que costaron unos servicios: la consulta, los exámenes y los
     * medicamentos al costo original, cuántos exámenes y medicamentos fueron,
     * el total, el descuento de la fundación y el neto que se le cobra al
     * familiar.
     */
    public record Costos(
            BigDecimal consulta,
            int examenes,
            BigDecimal costoExamenes,
            int medicamentos,
            BigDecimal costoMedicamentos,
            BigDecimal total,
            BigDecimal descuento,
            BigDecimal neto) {
    }
}
