package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import app.cabezadealgodon.com.gestionasilobackend.models.FormaPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * YS-133. Un pago del familiar (HU-34): la fecha, el monto y la forma de pago,
 * con la referencia de la boleta o la transferencia si la hay. Que no pase del
 * saldo lo revisa el servicio, porque depende de la cuenta.
 */
public record PagoRequest(
        @NotNull(message = "Indique el monto del pago")
        @DecimalMin(value = "0.00", inclusive = false, message = "El monto tiene que ser mayor que cero")
        @Digits(integer = 8, fraction = 2, message = "El monto admite hasta dos decimales")
        BigDecimal monto,

        @NotNull(message = "Indique la fecha del pago")
        @PastOrPresent(message = "La fecha del pago no puede estar en el futuro")
        LocalDate fecha,

        @NotNull(message = "Elija la forma de pago")
        FormaPago formaPago,

        @Size(max = 100, message = "La referencia no puede exceder 100 caracteres")
        String referencia) {
}
