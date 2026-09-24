package app.cabezadealgodon.com.gestionasilobackend.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * YS-52. Reserva el endpoint al Administrador del Asilo (ACT-04).
 *
 * <p>
 * Se declara como anotación propia y no como un {@code @PreAuthorize} suelto en
 * cada método por dos razones: la regla queda escrita una sola vez, de modo que
 * cambiar quién administra usuarios es un cambio en un archivo; y en el
 * controlador se lee el permiso en lugar de una expresión, que es donde los
 * errores de autorización pasan desapercibidos en una revisión.
 *
 * <p>
 * El nombre del rol viaja como texto porque una anotación solo admite
 * constantes, así que no hay forma de que el compilador lo ate a
 * {@link app.cabezadealgodon.com.gestionasilobackend.models.RolNombre}. Quien
 * renombre ese enum debe corregirlo aquí; las pruebas de acceso por rol son las
 * que detectan el descuido.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasRole('ADMINISTRADOR_ASILO')")
public @interface SoloAdministradorAsilo {
}
