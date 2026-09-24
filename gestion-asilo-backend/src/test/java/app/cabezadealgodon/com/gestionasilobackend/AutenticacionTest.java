package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import app.cabezadealgodon.com.gestionasilobackend.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * YS-51. Cubre el login, el rechazo de credenciales inválidas y la expiración
 * del token, que son los tres criterios de la subtarea, más los casos que hacen
 * falta para que esas comprobaciones signifiquen algo: un token manipulado, uno
 * firmado con otro secreto y el cierre de sesión.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Autenticación (YS-51)")
class AutenticacionTest extends PruebaConBaseDeDatos {

    private static final String ACTIVO = "enfermera.pruebas@asilo.gt";
    private static final String INACTIVA = "inactiva.pruebas@asilo.gt";
    private static final String PASSWORD = "Prueba1234";
    private static final String MENSAJE_GENERICO = "Credenciales inválidas";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String secreto;

    /**
     * Se reescriben en cada prueba, incluida la fecha de invalidación de
     * sesiones: sin eso, un cierre de sesión dejaría inservibles los tokens de
     * las pruebas siguientes según el orden en que se ejecuten.
     */
    @BeforeEach
    void prepararUsuarios() {
        guardarUsuario(ACTIVO, true);
        guardarUsuario(INACTIVA, false);
    }

    @Test
    @DisplayName("El login correcto devuelve el token con el nombre y el rol")
    void loginCorrecto() throws Exception {
        mvc.perform(peticionLogin(ACTIVO, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.correo").value(ACTIVO))
                .andExpect(jsonPath("$.rol").value(RolNombre.ENFERMERIA.name()));
    }

    @Test
    @DisplayName("El correo se acepta escrito en mayúsculas")
    void loginConCorreoEnMayusculas() throws Exception {
        mvc.perform(peticionLogin(ACTIVO.toUpperCase(), PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(ACTIVO));
    }

    /**
     * El corazón de la defensa contra enumeración de usuarios: si el mensaje
     * cambiara entre estos tres casos, bastaría con probar correos para saber
     * cuáles están registrados y cuáles están desactivados.
     */
    @Test
    @DisplayName("Contraseña incorrecta, correo inexistente y usuario desactivado responden lo mismo")
    void credencialesInvalidasNoRevelanNada() throws Exception {
        String porPassword = mensajeDelRechazo(ACTIVO, "OtraClave9999");
        String porCorreo = mensajeDelRechazo("no.registrado@asilo.gt", PASSWORD);
        String porDesactivado = mensajeDelRechazo(INACTIVA, PASSWORD);

        assertThat(porPassword).isEqualTo(MENSAJE_GENERICO);
        assertThat(porCorreo).isEqualTo(porPassword);
        assertThat(porDesactivado).isEqualTo(porPassword);
    }

    @Test
    @DisplayName("El login sin credenciales responde 400 con el error por campo")
    void loginSinCredenciales() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.correo").isNotEmpty())
                .andExpect(jsonPath("$.errores.password").isNotEmpty());
    }

    @Test
    @DisplayName("Un token válido abre la API y su ausencia la cierra")
    void tokenValidoYSinToken() throws Exception {
        mvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDeLogin()))
                .andExpect(status().isOk());

        mvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un token vencido se rechaza, y uno idéntico sin vencer se acepta")
    void tokenVencido() throws Exception {
        // Los dos tokens solo se diferencian en la expiración, así que el
        // rechazo no puede deberse a ninguna otra causa.
        mvc.perform(peticionConToken(tokenFirmadoCon(secreto, -1_000)))
                .andExpect(status().isUnauthorized());

        mvc.perform(peticionConToken(tokenFirmadoCon(secreto, 600_000)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un token con el contenido alterado después de firmarlo se rechaza")
    void tokenManipulado() throws Exception {
        String alterado = conRolAlterado(tokenDeLogin());

        mvc.perform(peticionConToken(alterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un token firmado con otro secreto se rechaza")
    void tokenFirmadoConOtroSecreto() throws Exception {
        String otroSecreto = "otro-secreto-distinto-igual-de-largo-0123456789";

        mvc.perform(peticionConToken(tokenFirmadoCon(otroSecreto, 600_000)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Al cerrar sesión el token deja de servir y volver a entrar entrega uno nuevo que sí sirve")
    void cierreDeSesion() throws Exception {
        String token = tokenDeLogin();

        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mvc.perform(peticionConToken(token))
                .andExpect(status().isUnauthorized());

        mvc.perform(peticionConToken(tokenDeLogin()))
                .andExpect(status().isOk());
    }

    // ---- apoyo ----

    private void guardarUsuario(String correo, boolean activo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElseGet(Usuario::new);
        usuario.setNombreCompleto("Persona de prueba");
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD));
        usuario.setRol(rolRepository.findByNombre(RolNombre.ENFERMERIA).orElseThrow());
        usuario.setActivo(activo);
        usuario.setFechaInvalidacionSesiones(null);
        usuarioRepository.save(usuario);
    }

    private RequestBuilder peticionLogin(String correo, String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"%s\", \"password\": \"%s\"}".formatted(correo, password));
    }

    private RequestBuilder peticionConToken(String token) {
        return get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String mensajeDelRechazo(String correo, String password) throws Exception {
        String cuerpo = mvc.perform(peticionLogin(correo, password))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(cuerpo, "$.mensaje");
    }

    private String tokenDeLogin() throws Exception {
        String cuerpo = mvc.perform(peticionLogin(ACTIVO, PASSWORD))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(cuerpo, "$.token");
    }

    private String tokenFirmadoCon(String secretoUsado, long milisegundosHastaVencer) {
        SecretKey clave = Keys.hmacShaKeyFor(secretoUsado.getBytes(StandardCharsets.UTF_8));
        Date ahora = new Date();
        return Jwts.builder()
                .subject(ACTIVO)
                .claim("rol", RolNombre.ENFERMERIA.name())
                .claim(JwtService.EMISION, ahora.getTime())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + milisegundosHastaVencer))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    /** Cambia el rol dentro de la carga y conserva la firma original. */
    private String conRolAlterado(String token) {
        String[] partes = token.split("\\.");
        String carga = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                .replace(RolNombre.ENFERMERIA.name(), RolNombre.ADMINISTRADOR_ASILO.name());
        String cargaAlterada = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(carga.getBytes(StandardCharsets.UTF_8));
        return partes[0] + "." + cargaAlterada + "." + partes[2];
    }
}
