package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import app.cabezadealgodon.com.gestionasilobackend.models.TipoGasto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * YS-148. Un gasto operativo (HU-23): el tipo, el monto y la fecha,
 * obligatorios, y la descripción, que lo es solo con el tipo Otro. Esa regla
 * depende de dos campos, así que la revisa el servicio.
 */
public record GastoOperativoRequest(
        @NotNull(message = "Elija el tipo de gasto")
        TipoGasto tipoGasto,

        @Size(max = 200, message = "La descripción no puede exceder 200 caracteres")
        String descripcion,

        @NotNull(message = "Indique el monto del gasto")
        @DecimalMin(value = "0.00", inclusive = false, message = "El monto tiene que ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "El monto admite hasta dos decimales")
        BigDecimal monto,

        @NotNull(message = "Indique la fecha del gasto")
        @PastOrPresent(message = "La fecha del gasto no puede estar en el futuro")
        LocalDate fecha) {
}
