package app.cabezadealgodon.com.gestionasilobackend.config;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * YS-96. El servidor de correo con que se avisa al familiar (RF-04).
 *
 * <p>
 * Se arma aquí y no se deja a la autoconfiguración de Spring Boot porque esa
 * solo crea el cliente si {@code spring.mail.host} está definido, y ese valor
 * vive en {@code application.properties}, que no se versiona: quien clone el
 * repositorio sin agregarlo tendría una aplicación que no arranca. Con estos
 * valores por defecto apunta a Mailpit en la máquina local, que es lo que
 * levanta {@code docker-compose.yml}.
 *
 * <p>
 * Se leen las mismas propiedades {@code spring.mail.*} de siempre, así que en
 * producción basta definirlas para apuntar a un servidor real, sin tocar
 * código (ver {@code application-example.properties}).
 */
@Configuration
public class CorreoConfig {

    /**
     * Cinco segundos y no los minutos que espera Jakarta Mail por defecto: si
     * el servidor de correo no responde, el envío tiene que fallar rápido,
     * quedar registrado como fallido y reintentarse después (YS-95).
     */
    private static final String ESPERA_MS = "5000";

    @Bean
    public JavaMailSender javaMailSender(
            @Value("${spring.mail.host:localhost}") String host,
            @Value("${spring.mail.port:1025}") int puerto,
            @Value("${spring.mail.username:}") String usuario,
            @Value("${spring.mail.password:}") String password,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}") boolean starttls) {
        JavaMailSenderImpl remitente = new JavaMailSenderImpl();
        remitente.setHost(host);
        remitente.setPort(puerto);
        remitente.setDefaultEncoding("UTF-8");

        Properties propiedades = remitente.getJavaMailProperties();
        if (!usuario.isBlank()) {
            remitente.setUsername(usuario);
            remitente.setPassword(password);
            propiedades.put("mail.smtp.auth", "true");
        }
        propiedades.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        propiedades.put("mail.smtp.connectiontimeout", ESPERA_MS);
        propiedades.put("mail.smtp.timeout", ESPERA_MS);
        propiedades.put("mail.smtp.writetimeout", ESPERA_MS);
        return remitente;
    }
}
