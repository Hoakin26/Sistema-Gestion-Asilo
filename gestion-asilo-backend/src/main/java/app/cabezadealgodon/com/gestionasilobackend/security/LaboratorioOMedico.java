package app.cabezadealgodon.com.gestionasilobackend.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * YS-117. Quienes pueden descargar el archivo de un resultado de laboratorio
 * (HU-13): el Laboratorio, que lo cargó, y los médicos, que lo consultan.
 * Decidido el 19/10.
 *
 * <p>
 * Es la primera barrera. La segunda está en {@code LaboratorioService}: un
 * especialista solo descarga el de sus pacientes. El Administrador del Asilo
 * no está, por el RNF-03: un resultado de laboratorio es un dato clínico.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasAnyRole('LABORATORIO', 'MEDICO_ESPECIALISTA', 'MEDICO_GENERAL')")
public @interface LaboratorioOMedico {
}
