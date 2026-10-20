package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import app.cabezadealgodon.com.gestionasilobackend.models.TipoDescuento;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;

/**
 * YS-136. Una versión de un descuento de la fundación (HU-19). {@code vigente}
 * dice si es la que rige hoy para su servicio; las demás son anteriores o
 * empiezan más adelante.
 */
public record PoliticaDescuentoResponse(
        Long id,
        TipoServicio tipoServicio,
        TipoDescuento tipoDescuento,
        BigDecimal valor,
        LocalDate vigenteDesde,
        boolean vigente,
        String registradaPor,
        LocalDateTime fechaRegistro) {
}
