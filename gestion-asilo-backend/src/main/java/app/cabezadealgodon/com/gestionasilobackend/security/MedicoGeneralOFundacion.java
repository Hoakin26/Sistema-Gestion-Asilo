package app.cabezadealgodon.com.gestionasilobackend.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * YS-84. Los dos extremos de una referencia: el Médico General, que la crea, y
 * el Administrador de la Fundación, que le asigna la cita (RF-03 y RF-05).
 *
 * <p>
 * Lo comparten los endpoints que ambos necesitan leer, como el catálogo de
 * especialidades y, con HU-05, el listado de solicitudes.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasAnyRole('MEDICO_GENERAL', 'ADMINISTRADOR_FUNDACION')")
public @interface MedicoGeneralOFundacion {
}
