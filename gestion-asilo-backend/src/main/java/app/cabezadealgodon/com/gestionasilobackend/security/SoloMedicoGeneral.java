package app.cabezadealgodon.com.gestionasilobackend.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * YS-58. Reserva el endpoint al Médico General (ACT-01).
 *
 * <p>
 * Es quien registra y actualiza el expediente de un interno: enfermería y los
 * especialistas lo consultan, pero no lo escriben.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasRole('MEDICO_GENERAL')")
public @interface SoloMedicoGeneral {
}
