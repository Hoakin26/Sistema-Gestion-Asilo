package app.cabezadealgodon.com.gestionasilobackend.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import app.cabezadealgodon.com.gestionasilobackend.security.EntryPointNoAutenticado;
import app.cabezadealgodon.com.gestionasilobackend.security.JwtAuthenticationFilter;

/** YS-46 y YS-47: cadena de filtros, CORS y reglas de acceso por ruta. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String origenFrontend;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final EntryPointNoAutenticado entryPointNoAutenticado;

    public SecurityConfig(@Value("${app.cors.origen-frontend:http://localhost:5173}") String origenFrontend,
            JwtAuthenticationFilter jwtAuthenticationFilter, EntryPointNoAutenticado entryPointNoAutenticado) {
        this.origenFrontend = origenFrontend;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.entryPointNoAutenticado = entryPointNoAutenticado;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracion) throws Exception {
        return configuracion.getAuthenticationManager();
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
                        // Solo el login es público: exigir autenticación para
                        // obtener un token sería un círculo sin salida. Cerrar
                        // sesión sí requiere token, porque cierra el del propio
                        // solicitante.
                        .requestMatchers("/api/auth/login").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(manejo -> manejo.authenticationEntryPoint(entryPointNoAutenticado))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
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
