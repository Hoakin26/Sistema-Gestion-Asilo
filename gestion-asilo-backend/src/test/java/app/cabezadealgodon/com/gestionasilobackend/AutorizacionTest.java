package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
 * YS-53 y YS-82. Recorre los siete roles del sistema contra la administración
 * de usuarios y contra los endpoints que son de todos.
 *
 * <p>
 * El barrido va sobre el enum {@link RolNombre} y no sobre una lista escrita a
 * mano, para que un rol nuevo entre solo a la prueba. Y la expectativa se
 * deriva del propio enum —solo ADMINISTRADOR_ASILO entra—, así que un rol nuevo
 * nace con la expectativa correcta: que se le niegue.
 *
 * <p>
 * Esto importa más de lo que parece. El nombre del rol viaja como texto dentro
 * de {@code @SoloAdministradorAsilo}, porque una anotación de Java solo admite
 * constantes y no hay forma de atarlo al enum. Si alguien renombra un valor de
 * {@code RolNombre}, nada falla al compilar y la restricción puede quedar rota
 * en silencio: estas pruebas son la única red.
 *
 * <p>
 * Se usan tokens reales obtenidos del login y no roles simulados con
 * anotaciones de Spring, para ejercitar la cadena completa —filtro JWT, carga
 * del usuario y autorización—, que es donde puede romperse.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Autorización por rol (YS-53, YS-82)")
class AutorizacionTest extends PruebaConBaseDeDatos {

    private static final String PASSWORD = "Prueba1234";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @ParameterizedTest(name = "{0}")
    @EnumSource(RolNombre.class)
    @DisplayName("Solo el Administrador del Asilo administra usuarios")
    void soloElAdministradorDelAsiloAdministraUsuarios(RolNombre rol) throws Exception {
        boolean deberiaEntrar = rol == RolNombre.ADMINISTRADOR_ASILO;

        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol)))
                .andExpect(deberiaEntrar ? status().isOk() : status().isForbidden());
    }

    /**
     * El contrapeso del barrido anterior. Restringir de más es tan defecto como
     * restringir de menos, y sin esto pasaría inadvertido: dejaría a seis de los
     * siete roles sin poder salir del sistema.
     */
    @ParameterizedTest(name = "{0}")
    @EnumSource(RolNombre.class)
    @DisplayName("Cerrar sesión está disponible para los siete roles")
    void cerrarSesionEsDeTodosLosRoles(RolNombre rol) throws Exception {
        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol)))
                .andExpect(status().isNoContent());
    }

    /**
     * El otro endpoint común a los siete: cada quien cambia su propia
     * contraseña. No confundir con restablecer la de otro, que sí es exclusivo
     * del Administrador del Asilo (YS-71).
     */
    @ParameterizedTest(name = "{0}")
    @EnumSource(RolNombre.class)
    @DisplayName("Cambiar la propia contraseña está disponible para los siete roles")
    void cambiarLaPropiaPasswordEsDeTodosLosRoles(RolNombre rol) throws Exception {
        String cuerpo = """
                {"passwordActual": "%s", "passwordNueva": "OtraClave9876"}
                """.formatted(PASSWORD);

        mvc.perform(put("/api/auth/password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isOk());
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
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ENFERMERIA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isForbidden());
    }

    /**
     * YS-54. Un 403 con el cuerpo vacío deja al frontend sin nada que mostrar y
     * al usuario ante una pantalla que no reacciona.
     */
    @Test
    @DisplayName("El 403 explica qué pasó, con el formato de error de la API")
    void elRechazoTraeMensajeYRuta() throws Exception {
        mvc.perform(get("/api/usuarios")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ENFERMERIA)))
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
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ENFERMERIA)))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(cuerpo).doesNotContain(RolNombre.ADMINISTRADOR_ASILO.name());
    }

    // ---- apoyo ----

    /**
     * Rehace el usuario del rol en cada llamada en vez de prepararlos una vez
     * al inicio. Hay pruebas que le cambian la contraseña y otras que le cierran
     * la sesión, así que un usuario compartido haría que el resultado dependiera
     * del orden en que JUnit ejecute las pruebas.
     */
    private String tokenDe(RolNombre rol) throws Exception {
        String correo = rol.name().toLowerCase(Locale.ROOT) + ".autorizacion@asilo.gt";

        Usuario usuario = usuarioRepository.findByCorreo(correo).orElseGet(Usuario::new);
        usuario.setNombreCompleto("Persona de prueba");
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD));
        usuario.setRol(rolRepository.findByNombre(rol).orElseThrow());
        usuario.setActivo(true);
        usuario.setFechaInvalidacionSesiones(null);
        usuarioRepository.save(usuario);

        String respuesta = mvc.perform(peticionLogin(correo))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(respuesta, "$.token");
    }

    private RequestBuilder peticionLogin(String correo) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"%s\", \"password\": \"%s\"}".formatted(correo, PASSWORD));
    }
}
