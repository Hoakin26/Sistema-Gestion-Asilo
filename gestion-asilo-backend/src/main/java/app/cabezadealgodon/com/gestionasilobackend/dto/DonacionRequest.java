package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import app.cabezadealgodon.com.gestionasilobackend.models.TipoDonante;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/** YS-145. Una donación (HU-22): el monto, la fecha, el donante y su tipo, todos obligatorios. */
public record DonacionRequest(
        @NotNull(message = "Indique el monto de la donación")
        @DecimalMin(value = "0.00", inclusive = false, message = "El monto tiene que ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "El monto admite hasta dos decimales")
        BigDecimal monto,

        @NotNull(message = "Indique la fecha de la donación")
        @PastOrPresent(message = "La fecha de la donación no puede estar en el futuro")
        LocalDate fecha,

        @NotBlank(message = "Escriba el nombre del donante")
        @Size(max = 150, message = "El nombre del donante no puede exceder 150 caracteres")
        String donante,

        @NotNull(message = "Elija el tipo de donante")
        TipoDonante tipoDonante) {
}
