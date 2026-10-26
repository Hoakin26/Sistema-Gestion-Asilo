package app.cabezadealgodon.com.gestionasilobackend;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLTransientConnectionException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.CannotCreateTransactionException;

import app.cabezadealgodon.com.gestionasilobackend.exception.BaseDeDatosNoDisponible;
import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.security.JwtService;
import app.cabezadealgodon.com.gestionasilobackend.security.UsuarioDetailsService;
import app.cabezadealgodon.com.gestionasilobackend.services.AuthService;

/**
 * DEF-03 (PP-08). Con la base caída, la API responde 503 con un mensaje claro.
 * Se simula la falla en vez de detener el contenedor: es uno solo para toda la
 * suite, y detenerlo tumbaría las pruebas que vienen después.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Con la base de datos caída, la API responde 503 con un mensaje claro (DEF-03)")
class BaseDeDatosCaidaTest extends PruebaConBaseDeDatos {

    private static final CannotCreateTransactionException SIN_CONEXION = new CannotCreateTransactionException(
            "Could not open JPA EntityManager for transaction",
            new SQLTransientConnectionException("Connection is not available, request timed out after 3000ms"));

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("Una petición con sesión responde 503 aunque falle en el filtro JWT, antes de Spring MVC")
    void peticionConSesion() throws Exception {
        when(usuarioDetailsService.loadUserByUsername(anyString())).thenThrow(SIN_CONEXION);
        String token = jwtService.generarToken("medico.caida@asilo.gt", RolNombre.MEDICO_GENERAL.name());

        mvc.perform(get("/api/internos").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "60"))
                .andExpect(jsonPath("$.mensaje").value(BaseDeDatosNoDisponible.MENSAJE));
    }

    @Test
    @DisplayName("El inicio de sesión responde 503, no 500 ni «Credenciales inválidas»")
    void inicioDeSesion() throws Exception {
        when(authService.login(any(), anyString())).thenThrow(SIN_CONEXION);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"medico.caida@asilo.gt\",\"password\":\"Prueba1234\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").value(BaseDeDatosNoDisponible.MENSAJE));
    }
}
