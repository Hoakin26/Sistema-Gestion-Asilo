package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDateTime;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoOrden;

/**
 * YS-111. Una orden de laboratorio, como la ve el especialista que la dio.
 * Sin el costo, por la misma razón que la visita no lleva el de la consulta.
 * HU-13 le agrega el resultado.
 */
public record OrdenLaboratorioResponse(
        Long id,
        Long visitaId,
        ExamenLaboratorioResponse examen,
        String indicaciones,
        EstadoOrden estado,
        LocalDateTime fechaOrden) {
}
