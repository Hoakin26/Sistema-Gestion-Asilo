package app.cabezadealgodon.com.gestionasilobackend.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * YS-58. Los roles que pueden leer datos clínicos: Médico General (ACT-01),
 * Médico Especialista (ACT-03) y Enfermería (ACT-05).
 *
 * <p>
 * <b>Esta anotación es el RNF-03 escrito como código.</b> Lo que importa es
 * quién <em>no</em> está: el Administrador del Asilo (ACT-04) no puede leer
 * psicopatologías, diagnósticos ni medicamentos, por mucho que sea el rol con
 * más permisos en el resto del sistema.
 *
 * <p>
 * Es la segunda barrera, no la única. La primera es que {@code Interno} no
 * tiene camino hacia {@code FichaMedica} (YS-57), de modo que un endpoint que
 * devuelva un interno no puede filtrar datos clínicos ni por descuido. Esta
 * anotación cubre los endpoints que sí los devuelven a propósito.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasAnyRole('MEDICO_GENERAL', 'MEDICO_ESPECIALISTA', 'ENFERMERIA')")
public @interface PersonalClinico {
}
