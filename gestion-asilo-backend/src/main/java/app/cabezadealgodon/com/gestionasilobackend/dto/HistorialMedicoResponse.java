package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDateTime;
import java.util.List;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoOrden;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoReceta;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoVisita;

/**
 * YS-102. Los antecedentes de un interno: lo que le pasó antes de la consulta
 * en curso (HU-08, RF-07). La ficha médica no va aquí: tiene su propio
 * endpoint, que el especialista puede leer siempre (YS-58).
 *
 * <p>
 * Decidido el 09/10: HU-08 se construye antes que la receta (HU-10) y los
 * exámenes (HU-11). Cada una de esas historias agregó aquí su lista: HU-10
 * las recetas y HU-11 los exámenes.
 *
 * <p>
 * HU-02 (YS-63) lo reutiliza para el historial centralizado del Médico
 * General, que agrupa todo por consulta: por eso la receta y cada examen dicen
 * de qué visita salieron.
 */
public record HistorialMedicoResponse(Long internoId, List<VisitaAnterior> visitas, List<RecetaAnterior> recetas,
        List<ExamenAnterior> examenes) {

    /**
     * YS-111. Un examen ordenado en una visita anterior, con quién lo pidió.
     * YS-117: con su resultado, desde que el laboratorio lo carga (HU-13).
     */
    public record ExamenAnterior(
            Long id,
            Long visitaId,
            LocalDateTime fechaOrden,
            EstadoOrden estado,
            ExamenLaboratorioResponse examen,
            String indicaciones,
            String numeroSolicitud,
            EspecialidadResponse especialidad,
            ResumenPersona especialista,
            String resultado,
            LocalDateTime fechaResultado,
            ArchivoResultadoResponse archivo) {
    }

    /** YS-108. Una receta de una visita anterior, con quién la emitió. */
    public record RecetaAnterior(
            Long id,
            Long visitaId,
            LocalDateTime fechaEmision,
            EstadoReceta estado,
            String numeroSolicitud,
            EspecialidadResponse especialidad,
            ResumenPersona especialista,
            String indicacionesGenerales,
            List<RecetaResponse.Medicamento> medicamentos) {
    }

    /**
     * Una visita anterior del interno, con cualquier especialista. El
     * diagnóstico y las observaciones los registra HU-09; una visita que
     * todavía no los tiene los trae vacíos.
     */
    public record VisitaAnterior(
            Long id,
            EstadoVisita estado,
            LocalDateTime fechaInicio,
            LocalDateTime fechaCierre,
            String numeroSolicitud,
            EspecialidadResponse especialidad,
            ResumenPersona especialista,
            String motivo,
            String diagnostico,
            String observaciones) {
    }
}
