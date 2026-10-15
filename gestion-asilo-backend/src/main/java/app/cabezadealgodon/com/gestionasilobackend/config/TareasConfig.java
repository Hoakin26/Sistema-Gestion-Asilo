package app.cabezadealgodon.com.gestionasilobackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * YS-94 y YS-95. Habilita el trabajo fuera de la petición:
 *
 * <ul>
 * <li>{@code @Async}: el correo al familiar se envía en otro hilo, para que
 * crear una solicitud no espere a un servidor de correo lento ni falle si está
 * caído (criterio de HU-04).</li>
 * <li>{@code @Scheduled}: los avisos fallidos se reintentan solos cada cierto
 * tiempo, sin que nadie tenga que acordarse.</li>
 * </ul>
 */
@Configuration
@EnableAsync
@EnableScheduling
public class TareasConfig {
}
