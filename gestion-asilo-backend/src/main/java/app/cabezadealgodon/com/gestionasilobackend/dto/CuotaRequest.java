package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/**
 * YS-132. La cuota base mensual del interno, en quetzales, que fija el
 * Administrador del Asilo (HU-18). Cero es "sin cuota": la cuenta del mes se
 * consolida solo con los cargos de servicios. Mismas reglas que al registrar
 * al interno (YS-59).
 */
public record CuotaRequest(
        @NotNull(message = "Indique la cuota mensual")
        @DecimalMin(value = "0.00", message = "La cuota mensual no puede ser negativa")
        @Digits(integer = 8, fraction = 2, message = "La cuota mensual admite hasta dos decimales")
        BigDecimal cuotaMensual) {
}
