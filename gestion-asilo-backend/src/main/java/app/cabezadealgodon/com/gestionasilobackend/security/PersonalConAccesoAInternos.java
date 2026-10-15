package app.cabezadealgodon.com.gestionasilobackend.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * YS-58. Los roles que pueden consultar el expediente administrativo de un
 * interno: Médico General, Médico Especialista, Enfermería y el Administrador
 * del Asilo.
 *
 * <p>
 * El Administrador del Asilo sí entra aquí, y debe entrar: necesita los datos
 * personales, el familiar responsable y el estado de cuenta para su trabajo.
 * Lo que no puede ver son los datos clínicos, y de eso se encarga
 * {@link PersonalClinico} en los endpoints de la ficha médica.
 *
 * <p>
 * Quedan fuera la Fundación, el Laboratorio y la Farmacia, que trabajan sobre
 * solicitudes, órdenes y recetas, no sobre el expediente del interno.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasAnyRole('MEDICO_GENERAL', 'MEDICO_ESPECIALISTA', 'ENFERMERIA', 'ADMINISTRADOR_ASILO')")
public @interface PersonalConAccesoAInternos {
}
