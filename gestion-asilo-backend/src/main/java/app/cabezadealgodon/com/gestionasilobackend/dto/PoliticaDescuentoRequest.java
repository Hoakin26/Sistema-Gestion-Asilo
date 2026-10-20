package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import app.cabezadealgodon.com.gestionasilobackend.models.TipoDescuento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/**
 * YS-136. Un descuento de la fundación para un servicio, desde una fecha
 * (HU-19). Cero es "sin descuento". Que un porcentaje no pase de 100 lo
 * revisa el servicio, porque depende del tipo.
 */
public record PoliticaDescuentoRequest(
        @NotNull(message = "Elija si el descuento es un porcentaje o un monto fijo")
        TipoDescuento tipoDescuento,

        @NotNull(message = "Indique el valor del descuento")
        @DecimalMin(value = "0.00", message = "El descuento no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El descuento admite hasta dos decimales")
        BigDecimal valor,

        @NotNull(message = "Indique desde qué fecha rige")
        LocalDate vigenteDesde) {
}
