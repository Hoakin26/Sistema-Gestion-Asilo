package app.cabezadealgodon.com.gestionasilobackend.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * YS-99. Reserva el endpoint al Médico Especialista (ACT-03): su agenda y sus
 * visitas.
 *
 * <p>
 * El rol es solo la primera puerta. Que la cita o la visita sea de ese
 * especialista y no de otro lo comprueba el servicio, porque depende del
 * registro y no del rol.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasRole('MEDICO_ESPECIALISTA')")
public @interface SoloMedicoEspecialista {
}
