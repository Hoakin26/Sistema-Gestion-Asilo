package app.cabezadealgodon.com.gestionasilobackend.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import tools.jackson.databind.ObjectMapper;

import app.cabezadealgodon.com.gestionasilobackend.security.CambioPasswordObligatorioFilter;
import app.cabezadealgodon.com.gestionasilobackend.security.EntryPointNoAutenticado;
import app.cabezadealgodon.com.gestionasilobackend.security.JwtAuthenticationFilter;
import app.cabezadealgodon.com.gestionasilobackend.security.ManejadorAccesoDenegado;

/**
 * YS-46 y YS-47: cadena de filtros, CORS y reglas de acceso por ruta.
 *
 * <p>
 * YS-52. {@code @EnableMethodSecurity} habilita la autorización por rol
 * anotada en los controladores. Se eligió eso en lugar de enumerar las rutas
 * aquí porque la regla queda junto al endpoint que protege: una ruta nueva
 * nace con su permiso a la vista, mientras que una lista central se olvida de
 * actualizar y deja el endpoint abierto sin que nada lo señale.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final String origenFrontend;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final EntryPointNoAutenticado entryPointNoAutenticado;
    private final ManejadorAccesoDenegado manejadorAccesoDenegado;
    private final ObjectMapper objectMapper;

    public SecurityConfig(@Value("${app.cors.origen-frontend:http://localhost:5173}") String origenFrontend,
            JwtAuthenticationFilter jwtAuthenticationFilter, EntryPointNoAutenticado entryPointNoAutenticado,
            ManejadorAccesoDenegado manejadorAccesoDenegado, ObjectMapper objectMapper) {
        this.origenFrontend = origenFrontend;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.entryPointNoAutenticado = entryPointNoAutenticado;
        this.manejadorAccesoDenegado = manejadorAccesoDenegado;
        this.objectMapper = objectMapper;
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
                // YS-54. Sin el manejador de acceso denegado, una denegación
                // que ocurra en la cadena de filtros sale como 403 sin cuerpo.
                .exceptionHandling(manejo -> manejo
                        .authenticationEntryPoint(entryPointNoAutenticado)
                        .accessDeniedHandler(manejadorAccesoDenegado))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                /*
                 * YS-74. Después del filtro JWT, que es el que deja al usuario
                 * cargado en el contexto: antes de él no habría a quién
                 * comprobarle la marca.
                 *
                 * Se construye aquí en vez de inyectarse como bean porque
                 * Spring Boot registra automáticamente todo bean de tipo Filter
                 * en el contenedor de servlets, y el filtro terminaría
                 * corriendo fuera de esta cadena —sin usuario autenticado— y
                 * saltándose la de adentro. Ver el comentario de la clase.
                 */
                .addFilterAfter(new CambioPasswordObligatorioFilter(objectMapper), JwtAuthenticationFilter.class)
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
