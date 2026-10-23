package app.cabezadealgodon.com.gestionasilobackend.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * YS-154. Los dos médicos: el Médico General y el Médico Especialista. Para el
 * reporte de análisis médico (HU-25), el de más información clínica del
 * sistema. Ni enfermería ni el Administrador del Asilo (RNF-03). El servicio
 * restringe además al especialista al paciente de su visita activa.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasAnyRole('MEDICO_GENERAL', 'MEDICO_ESPECIALISTA')")
public @interface MedicoGeneralOEspecialista {
}
