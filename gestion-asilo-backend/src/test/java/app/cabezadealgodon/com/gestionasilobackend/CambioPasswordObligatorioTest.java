package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * YS-76. Cubre HU-32 de punta a punta: que quien recibe una contraseña elegida
 * por otro quede obligado a cambiarla, que no pueda usar el sistema hasta
 * hacerlo, y que al cambiarla recupere el acceso.
 *
 * <p>
 * <b>Los dos usuarios de estas pruebas son Administradores del Asilo a
 * propósito.</b> El endpoint con el que se comprueba el bloqueo es
 * {@code GET /api/usuarios}, que está reservado a ese rol (YS-53): si el
 * usuario bloqueado fuera de enfermería, su 403 podría venir del guardia de
 * roles y la prueba pasaría sin haber comprobado nada de HU-32. Siendo
 * administrador, el único 403 posible es el del cambio de contraseña
 * pendiente, y aun así se verifica el código del cuerpo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Contraseña temporal obligatoria (YS-76)")
class CambioPasswordObligatorioTest extends PruebaConBaseDeDatos {

    /** Hace los restablecimientos. No puede restablecerse a sí mismo (YS-71). */
    private static final String ADMIN = "admin.hu32@asilo.gt";
    /** El que los recibe. */
    private static final String OBJETIVO = "objetivo.hu32@asilo.gt";

    private static final String PASSWORD = "Prueba1234";
    private static final String TEMPORAL = "Temporal9876";
    private static final String ELEGIDA = "Elegida5432";
    private static final String CODIGO = "CAMBIO_PASSWORD_REQUERIDO";

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
        guardarUsuario(ADMIN);
        guardarUsuario(OBJETIVO);
    }

    @Test
    @DisplayName("Restablecer la contraseña de alguien lo deja obligado a cambiarla")
    void elRestablecimientoDejaLaMarca() throws Exception {
        restablecer(TEMPORAL);

        mvc.perform(peticionLogin(OBJETIVO, TEMPORAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debeCambiarPassword").value(true));
    }

    @Test
    @DisplayName("Con la marca puesta, el sistema se cierra salvo el cambio y el cierre de sesión")
    void conLaMarcaPuestaElSistemaSeCierra() throws Exception {
        restablecer(TEMPORAL);
        String token = tokenDe(OBJETIVO, TEMPORAL);

        // Su rol sí tiene permiso sobre esta ruta: el 403 solo puede venir del
        // cambio de contraseña pendiente.
        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(CODIGO));

        // Y las dos salidas siguen abiertas, porque si no quedaría encerrado.
        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    /**
     * El recorrido completo, que es el que de verdad importa. Comprobar cada
     * pieza por separado dejaría pasar el caso en que todas funcionan y aun así
     * el usuario queda encerrado sin poder salir.
     */
    @Test
    @DisplayName("Restablecer, entrar, chocar, cambiar y volver a trabajar")
    void elCircuitoCompleto() throws Exception {
        restablecer(TEMPORAL);

        String conTemporal = tokenDe(OBJETIVO, TEMPORAL);
        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + conTemporal))
                .andExpect(status().isForbidden());

        String cuerpo = mvc.perform(peticionCambio(conTemporal, TEMPORAL, ELEGIDA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debeCambiarPassword").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        // Con el token que devuelve el cambio, el sistema vuelve a responder.
        String nuevo = JsonPath.read(cuerpo, "$.token");
        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + nuevo))
                .andExpect(status().isOk());
    }

    /**
     * El cambio no puede cumplirse dejando puesta la contraseña que el
     * administrador dictó: sin esta regla la obligación sería decorativa.
     */
    @Test
    @DisplayName("No se puede cumplir el cambio repitiendo la contraseña temporal")
    void repetirLaTemporalNoCumple() throws Exception {
        restablecer(TEMPORAL);
        String token = tokenDe(OBJETIVO, TEMPORAL);

        mvc.perform(peticionCambio(token, TEMPORAL, TEMPORAL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.passwordNueva").isNotEmpty());

        assertThat(marcaDe(OBJETIVO)).isTrue();
    }

    @Test
    @DisplayName("Cambiar la contraseña por decisión propia no deja marca")
    void elCambioVoluntarioNoDejaMarca() throws Exception {
        mvc.perform(peticionCambio(tokenDe(OBJETIVO, PASSWORD), PASSWORD, ELEGIDA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debeCambiarPassword").value(false));

        assertThat(marcaDe(OBJETIVO)).isFalse();
    }

    /**
     * YS-71 ya invalidaba las sesiones al restablecer. Se comprueba aquí porque
     * es lo que impide que quien tuviera la sesión abierta siga trabajando sin
     * pasar nunca por la obligación de cambiarla.
     */
    @Test
    @DisplayName("El restablecimiento invalida las sesiones que ya estaban abiertas")
    void elRestablecimientoCierraLasSesionesAbiertas() throws Exception {
        String antesDelRestablecimiento = tokenDe(OBJETIVO, PASSWORD);
        restablecer(TEMPORAL);

        // 401 y no 403: ese token ya no vale, no es que le falte cambiar nada.
        mvc.perform(get("/api/usuarios")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + antesDelRestablecimiento))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Fuera de lo que pedían los criterios de YS-72, que solo nombran el
     * restablecimiento. Se incluye porque el alta produce el mismo problema por
     * el camino más transitado: el administrador escribe una contraseña que
     * después conoce.
     */
    @Test
    @DisplayName("Un usuario dado de alta nace obligado a cambiar su contraseña")
    void elAltaTambienDejaLaMarca() throws Exception {
        String alta = """
                {"nombreCompleto": "Recien Creada",
                 "correo": "recien.hu32@asilo.gt",
                 "password": "%s",
                 "rol": "ENFERMERIA"}
                """.formatted(TEMPORAL);

        mvc.perform(post("/api/usuarios")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(ADMIN, PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(alta))
                .andExpect(status().isCreated());

        mvc.perform(peticionLogin("recien.hu32@asilo.gt", TEMPORAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debeCambiarPassword").value(true));
    }

    // ---- apoyo ----

    /**
     * Rehace el usuario en cada prueba, con la marca apagada y sin fecha de
     * invalidación. Sin eso, un restablecimiento o un cierre de sesión dejarían
     * inservibles los tokens de las pruebas siguientes según el orden en que
     * JUnit las ejecute.
     */
    private void guardarUsuario(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElseGet(Usuario::new);
        usuario.setNombreCompleto("Persona de prueba");
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD));
        usuario.setRol(rolRepository.findByNombre(RolNombre.ADMINISTRADOR_ASILO).orElseThrow());
        usuario.setActivo(true);
        usuario.setFechaInvalidacionSesiones(null);
        usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);
    }

    /** El administrador le asigna una contraseña temporal al objetivo. */
    private void restablecer(String temporal) throws Exception {
        Long id = usuarioRepository.findByCorreo(OBJETIVO).orElseThrow().getId();

        mvc.perform(put("/api/usuarios/{id}/password", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(ADMIN, PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\": \"%s\"}".formatted(temporal)))
                .andExpect(status().isOk());
    }

    private boolean marcaDe(String correo) {
        return usuarioRepository.findByCorreo(correo).orElseThrow().isDebeCambiarPassword();
    }

    private RequestBuilder peticionCambio(String token, String actual, String nueva) {
        return put("/api/auth/password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"passwordActual\": \"%s\", \"passwordNueva\": \"%s\"}"
                        .formatted(actual, nueva));
    }

    private RequestBuilder peticionLogin(String correo, String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"%s\", \"password\": \"%s\"}".formatted(correo, password));
    }

    private String tokenDe(String correo, String password) throws Exception {
        String cuerpo = mvc.perform(peticionLogin(correo, password))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(cuerpo, "$.token");
    }
}
