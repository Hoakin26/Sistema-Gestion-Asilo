package app.cabezadealgodon.com.gestionasilobackend.dto.validacion;

import app.cabezadealgodon.com.gestionasilobackend.dto.InternoRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** YS-59. Ver {@link IngresoPosteriorANacimiento}. */
public class IngresoPosteriorANacimientoValidator
        implements ConstraintValidator<IngresoPosteriorANacimiento, InternoRequest> {

    @Override
    public boolean isValid(InternoRequest request, ConstraintValidatorContext contexto) {
        /*
         * Si falta alguna de las dos fechas no hay nada que comparar: ese caso
         * ya lo reporta el @NotNull de cada campo. Reportarlo también aquí
         * mostraría dos errores por el mismo motivo.
         */
        if (request == null || request.fechaNacimiento() == null || request.fechaIngreso() == null) {
            return true;
        }

        // El mismo día se acepta: un recién nacido no ingresa a un asilo, pero
        // la regla protege contra fechas invertidas, no contra edades.
        if (!request.fechaIngreso().isBefore(request.fechaNacimiento())) {
            return true;
        }

        contexto.disableDefaultConstraintViolation();
        contexto.buildConstraintViolationWithTemplate(contexto.getDefaultConstraintMessageTemplate())
                .addPropertyNode("fechaIngreso")
                .addConstraintViolation();
        return false;
    }
}
