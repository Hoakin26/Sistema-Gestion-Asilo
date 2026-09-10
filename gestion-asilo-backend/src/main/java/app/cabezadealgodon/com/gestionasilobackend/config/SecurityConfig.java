package app.cabezadealgodon.com.gestionasilobackend.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** YS-46: cadena de filtros, CORS y reglas de acceso por ruta. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String origenFrontend;

    public SecurityConfig(@Value("${app.cors.origen-frontend:http://localhost:5173}") String origenFrontend) {
        this.origenFrontend = origenFrontend;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // La API no autentica con cookies de sesión, así que un sitio
                // externo no puede forjar peticiones con las credenciales del
                // usuario: la protección CSRF no aplica y solo bloquearía las
                // escrituras de clientes como el frontend o Postman.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        // El login vive aquí y debe ser público (YS-47).
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated())
                // Autenticación provisional hasta que YS-47 agregue el filtro JWT.
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(List.of(origenFrontend));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }
}
