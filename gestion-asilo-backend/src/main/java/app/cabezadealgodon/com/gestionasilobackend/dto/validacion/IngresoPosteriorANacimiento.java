package app.cabezadealgodon.com.gestionasilobackend.dto.validacion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * YS-59. Nadie ingresa al asilo antes de haber nacido.
 *
 * <p>
 * Parece obvio, pero es justo el error que se comete al cargar fechas a mano:
 * invertir el día de ingreso con el de nacimiento, o equivocar un dígito del
 * año. Ninguna de las dos fechas es inválida por sí sola, así que la regla no
 * puede vivir en un campo; tiene que mirar las dos a la vez.
 *
 * <p>
 * El error se reporta sobre {@code fechaIngreso} y no sobre el objeto entero,
 * para que el formulario lo muestre junto al campo que el usuario tiene que
 * corregir, igual que el resto de los errores de la API.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = IngresoPosteriorANacimientoValidator.class)
public @interface IngresoPosteriorANacimiento {

    String message() default "La fecha de ingreso no puede ser anterior a la fecha de nacimiento";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
