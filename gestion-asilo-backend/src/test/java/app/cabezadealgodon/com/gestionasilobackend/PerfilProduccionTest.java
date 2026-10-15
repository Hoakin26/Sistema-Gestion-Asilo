package app.cabezadealgodon.com.gestionasilobackend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.RolRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;

/**
 * YS-174. El perfil de producción, con el {@code application-prod.properties}
 * de verdad: la salud del sistema se consulta sin token y sin detalles, y
 * Actuator no expone nada más.
 *
 * <p>
 * La base de datos sigue siendo la de Testcontainers: las variables de entorno
 * del servidor no existen aquí, y {@link PruebaConBaseDeDatos} pone las suyas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@DisplayName("Perfil de producción (YS-174)")
class PerfilProduccionTest extends PruebaConBaseDeDatos {

    private static final String PASSWORD = "Prueba1234";
    private static final String CORREO = "administrador.asilo.produccion@asilo.gt";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("La salud responde UP sin token y sin detalles internos")
    void saludSinTokenNiDetalles() throws Exception {
        mvc.perform(get("/api/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                // Ni el estado de la base ni el del disco: solo UP o DOWN.
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.groups").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("Actuator no expone nada más que la salud, ni siquiera con sesión")
    void soloLaSalud() throws Exception {
        String token = token();
        for (String ruta : new String[] { "/api/actuator/env", "/api/actuator/beans", "/api/actuator/info",
                "/api/actuator/configprops", "/actuator/health" }) {
            mvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("Sin token, el resto de Actuator responde 401: ni siquiera se revela qué existe")
    void sinTokenSoloLaSalud() throws Exception {
        mvc.perform(get("/api/actuator/env")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/actuator")).andExpect(status().isUnauthorized());
    }

    private String token() throws Exception {
        Usuario usuario = usuarioRepository.findByCorreo(CORREO).orElseGet(Usuario::new);
        usuario.setNombreCompleto("Persona de prueba");
        usuario.setCorreo(CORREO);
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD));
        usuario.setRol(rolRepository.findByNombre(RolNombre.ADMINISTRADOR_ASILO).orElseThrow());
        usuario.setActivo(true);
        usuario.setFechaInvalidacionSesiones(null);
        usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);

        String respuesta = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"%s\", \"password\": \"%s\"}".formatted(CORREO, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(respuesta, "$.token");
    }
}
