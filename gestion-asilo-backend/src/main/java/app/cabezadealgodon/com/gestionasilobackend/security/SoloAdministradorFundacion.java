package app.cabezadealgodon.com.gestionasilobackend.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * YS-167 y YS-91. Reserva el endpoint al Administrador de la Fundación
 * (ACT-02): quien registra a los especialistas y asigna las citas (RF-05).
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasRole('ADMINISTRADOR_FUNDACION')")
public @interface SoloAdministradorFundacion {
}
