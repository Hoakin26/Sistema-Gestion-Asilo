package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDateTime;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoOrden;

/**
 * YS-111. Una orden de laboratorio. Sin el costo, por la misma razón que la
 * visita no lleva el de la consulta. HU-13 le agrega el resultado.
 *
 * <p>
 * YS-114. Con el paciente y el médico que la solicitó, que es lo que la
 * bandeja del laboratorio muestra de cada orden (HU-12). Nada del diagnóstico:
 * el laboratorio ve la orden, no la consulta (mínimo privilegio, sección 4.9
 * del diseño). Los dos van como {@link ResumenPersona}, sin el DPI.
 *
 * <p>
 * YS-117. Con el resultado, desde que el laboratorio lo carga (HU-13): el
 * texto, la fecha y el archivo adjunto, si lo hay. Antes, los tres van vacíos.
 */
public record OrdenLaboratorioResponse(
        Long id,
        Long visitaId,
        ResumenPersona paciente,
        ResumenPersona medicoSolicitante,
        ExamenLaboratorioResponse examen,
        String indicaciones,
        EstadoOrden estado,
        LocalDateTime fechaOrden,
        String resultado,
        LocalDateTime fechaResultado,
        ArchivoResultadoResponse archivo) {
}
