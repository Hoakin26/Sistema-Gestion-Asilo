package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import app.cabezadealgodon.com.gestionasilobackend.dto.validacion.IngresoPosteriorANacimiento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * YS-58. Los datos con que se registra o actualiza un interno.
 *
 * <p>
 * <b>No incluye datos clínicos.</b> La ficha médica se escribe por su propio
 * endpoint, reservado al Médico General: si también se pudiera escribir desde
 * aquí habría dos caminos hacia el mismo dato, y bastaría olvidar uno al
 * cambiar una regla para que las dos versiones empezaran a discrepar.
 *
 * <p>
 * YS-59. Las reglas de cada campo van en el campo; la que compara dos fechas
 * entre sí va en la clase, porque ningún campo por separado puede saber si es
 * válido sin mirar al otro.
 */
@IngresoPosteriorANacimiento
public record InternoRequest(
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 150, message = "El nombre completo no puede exceder 150 caracteres")
        String nombreCompleto,

        @NotBlank(message = "El DPI es obligatorio")
        @Pattern(regexp = "\\d{13}", message = "El DPI debe tener exactamente 13 dígitos")
        String dpi,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @PastOrPresent(message = "La fecha de nacimiento no puede estar en el futuro")
        LocalDate fechaNacimiento,

        @NotNull(message = "La fecha de ingreso es obligatoria")
        @PastOrPresent(message = "La fecha de ingreso no puede estar en el futuro")
        LocalDate fechaIngreso,

        // YS-59. Opcional: lo registra el Médico General y la cuota no es de su
        // competencia (ver el comentario de Interno). Las dos reglas de abajo
        // solo se aplican si viene; sobre un nulo Bean Validation no evalúa nada.
        // Cero es válido: hay internos becados por la fundación.
        @DecimalMin(value = "0.00", message = "La cuota mensual no puede ser negativa")
        @Digits(integer = 8, fraction = 2, message = "La cuota mensual admite hasta dos decimales")
        BigDecimal cuotaMensual,

        /*
         * @Valid propaga la validación al objeto anidado. Sin él, un familiar
         * con todos los campos vacíos pasaría sin que nada lo señalara.
         */
        @NotNull(message = "El familiar responsable es obligatorio")
        @Valid
        FamiliarRequest familiarResponsable) {
}
