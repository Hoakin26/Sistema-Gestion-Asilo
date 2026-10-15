package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** YS-169. La tarifa de consulta de una especialidad, en quetzales. */
public record TarifaRequest(
        @NotNull(message = "Indique la tarifa de consulta")
        @DecimalMin(value = "0.00", message = "La tarifa no puede ser negativa")
        @Digits(integer = 8, fraction = 2, message = "La tarifa admite hasta 8 cifras y 2 decimales")
        BigDecimal tarifaConsulta) {
}
