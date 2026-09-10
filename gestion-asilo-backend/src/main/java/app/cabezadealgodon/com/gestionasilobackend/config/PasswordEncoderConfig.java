package app.cabezadealgodon.com.gestionasilobackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    /**
     * Encoder delegado: cifra con BCrypt y guarda el hash con el prefijo
     * {bcrypt}, lo que permite verificar contraseñas cifradas con otro
     * algoritmo si en el futuro se migra (ver docs/seguridad.md).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
