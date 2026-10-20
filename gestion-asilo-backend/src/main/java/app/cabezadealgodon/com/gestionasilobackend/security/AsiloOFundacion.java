package app.cabezadealgodon.com.gestionasilobackend.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * YS-136. Los dos administradores: la fundación, que define sus descuentos, y
 * el asilo, que los consulta porque los aplica a la cuenta del familiar
 * (HU-19). Solo para leer; definirlos es {@link SoloAdministradorFundacion}.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasAnyRole('ADMINISTRADOR_ASILO', 'ADMINISTRADOR_FUNDACION')")
public @interface AsiloOFundacion {
}
