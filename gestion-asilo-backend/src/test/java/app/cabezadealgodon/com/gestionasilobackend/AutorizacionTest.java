package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import com.jayway.jsonpath.JsonPath;

import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.RolRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;

/**
 * YS-53. Comprueba que la restricción por rol distinga entre "no tiene sesión"
 * y "tiene sesión pero esto no le corresponde", y que no se haya pasado de
 * largo cerrando endpoints que sí son de todos los roles.
 *
 * <p>
 * Las tres pruebas usan tokens reales obtenidos del login, no roles simulados
 * con anotaciones de Spring: así se ejercita la cadena completa —filtro JWT,
 * carga del usuario y autorización— que es donde puede romperse.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Autorización por rol (YS-53)")
class AutorizacionTest extends PruebaConBaseDeDatos {

    private static final String ENFERMERA = "enfermera.autorizacion@asilo.gt";
    private static final String ADMIN_ASILO = "admin.autorizacion@asilo.gt";
    private static final String PASSWORD = "Prueba1234";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void prepararUsuarios() {
        guardarUsuario(ENFERMERA, RolNombre.ENFERMERIA);
        guardarUsuario(ADMIN_ASILO, RolNombre.ADMINISTRADOR_ASILO);
    }

    @Test
    @DisplayName("Un rol sin permiso recibe 403 aunque su token sea válido")
    void rolSinPermisoRecibe403() throws Exception {
        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(ENFERMERA)))
                .andExpect(status().isForbidden());
    }

    /**
     * YS-54. Un 403 con el cuerpo vacío deja al frontend sin nada que mostrar y
     * al usuario ante una pantalla que no reacciona. La respuesta debe traer el
     * mismo formato de error que el resto de la API.
     */
    @Test
    @DisplayName("El 403 explica qué pasó, con el formato de error de la API")
    void elRechazoTraeMensajeYRuta() throws Exception {
        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(ENFERMERA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.ruta").value("/api/usuarios"));
    }

    /**
     * El mensaje no debe nombrar el rol que haría falta: eso le iría
     * describiendo el mapa de permisos a quien prueba dónde puede entrar.
     */
    @Test
    @DisplayName("El mensaje del 403 no revela qué rol se necesita")
    void elMensajeNoRevelaElRolNecesario() throws Exception {
        String cuerpo = mvc.perform(get("/api/usuarios")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(ENFERMERA)))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(cuerpo).doesNotContain(RolNombre.ADMINISTRADOR_ASILO.name());
    }

    /**
     * El 403 y el 401 no son intercambiables: el frontend trata todo 401 como
     * sesión terminada y expulsa al usuario al login. Si esto respondiera 401,
     * enfermería quedaría en un ciclo de expulsión sin entender por qué.
     */
    @Test
    @DisplayName("Sin token la misma ruta responde 401, no 403")
    void sinTokenResponde401() throws Exception {
        mvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El Administrador del Asilo sí entra a la administración de usuarios")
    void administradorDelAsiloEntra() throws Exception {
        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(ADMIN_ASILO)))
                .andExpect(status().isOk());
    }

    /**
     * La restricción está puesta en la clase del controlador, así que cubre
     * todos sus métodos. Se comprueba con una escritura además de la lectura
     * porque es la que causaría daño si quedara abierta.
     */
    @Test
    @DisplayName("La restricción alcanza también a la creación de usuarios")
    void crearUsuarioTambienEstaRestringido() throws Exception {
        String cuerpo = """
                {"nombreCompleto": "Intento no autorizado",
                 "correo": "intento@asilo.gt",
                 "password": "Prueba1234",
                 "rol": "FARMACIA"}
                """;

        mvc.perform(post("/api/usuarios")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(ENFERMERA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isForbidden());
    }

    /**
     * El contrapeso de las pruebas anteriores: cerrar sesión es común a los
     * siete roles. Sin esta comprobación, restringir de más pasaría inadvertido
     * y dejaría a seis de los siete roles sin poder salir del sistema.
     */
    @Test
    @DisplayName("Cerrar sesión sigue disponible para un rol sin acceso a usuarios")
    void cerrarSesionSigueSiendoDeTodos() throws Exception {
        mvc.perform(post("/api/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(ENFERMERA)))
                .andExpect(status().isNoContent());
    }

    // ---- apoyo ----

    private void guardarUsuario(String correo, RolNombre rol) {
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElseGet(Usuario::new);
        usuario.setNombreCompleto("Persona de prueba");
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD));
        usuario.setRol(rolRepository.findByNombre(rol).orElseThrow());
        usuario.setActivo(true);
        usuario.setFechaInvalidacionSesiones(null);
        usuarioRepository.save(usuario);
    }

    private String tokenDe(String correo) throws Exception {
        String cuerpo = mvc.perform(peticionLogin(correo))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(cuerpo, "$.token");
    }

    private RequestBuilder peticionLogin(String correo) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"%s\", \"password\": \"%s\"}".formatted(correo, PASSWORD));
    }
}
