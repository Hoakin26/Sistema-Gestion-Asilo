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
 * YS-58. Los endpoints del expediente del interno y, sobre todo, quién puede
 * usar cada uno.
 *
 * <p>
 * La prueba que no puede faltar es la del RNF-03: el Administrador del Asilo
 * entra al expediente administrativo pero no a la ficha médica. Es la única
 * restricción del sistema que protege datos clínicos de una persona, y es la
 * que más fácil se rompe sin que nadie lo note, porque ese rol tiene permiso
 * para casi todo lo demás.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Expediente del interno (YS-58)")
class InternoApiTest extends PruebaConBaseDeDatos {

    private static final String PASSWORD = "Prueba1234";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("El Médico General registra un interno y la respuesta no trae datos clínicos")
    void registraSinDevolverDatosClinicos() throws Exception {
        String cuerpo = mvc.perform(peticionAlta("1111111111111", RolNombre.MEDICO_GENERAL))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreCompleto").value("Rosa Elena Morales"))
                .andExpect(jsonPath("$.familiarResponsable.parentesco").value("Hija"))
                // Lo que no debe venir. Si alguien agregara la ficha a este DTO,
                // el Administrador del Asilo leería datos clínicos sin que
                // ninguna anotación de rol lo impidiera.
                .andExpect(jsonPath("$.fichaMedica").doesNotExist())
                .andExpect(jsonPath("$.psicopatologias").doesNotExist())
                .andExpect(jsonPath("$.medicamentosHabituales").doesNotExist())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(cuerpo).doesNotContain("tipoSangre", "alergias", "motivoIngreso");
    }

    /**
     * El corazón del RNF-03, en dos peticiones del mismo usuario: entra al
     * expediente y rebota en la ficha.
     */
    @Test
    @DisplayName("El Administrador del Asilo ve el expediente pero no la ficha médica")
    void elAdministradorDelAsiloNoAlcanzaLoClinico() throws Exception {
        Long id = registrarInterno("2222222222222");
        String token = tokenDe(RolNombre.ADMINISTRADOR_ASILO);

        mvc.perform(get("/api/internos/{id}", id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dpi").value("2222222222222"));

        mvc.perform(get("/api/internos/{id}/ficha-medica", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = { "MEDICO_GENERAL", "MEDICO_ESPECIALISTA", "ENFERMERIA" })
    @DisplayName("El personal clínico sí puede leer la ficha médica")
    void elPersonalClinicoLeeLaFicha(RolNombre rol) throws Exception {
        Long id = registrarInterno("33333333333" + String.format("%02d", rol.ordinal()));

        mvc.perform(get("/api/internos/{id}/ficha-medica", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.internoId").value(id));
    }

    /**
     * El contrapeso: restringir de más también es un defecto. Enfermería y los
     * especialistas tienen que poder consultar el expediente.
     */
    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = { "ADMINISTRADOR_FUNDACION", "LABORATORIO", "FARMACIA" })
    @DisplayName("Los roles ajenos al expediente no entran ni a la lista")
    void losRolesAjenosNoEntran(RolNombre rol) throws Exception {
        mvc.perform(get("/api/internos").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Solo el Médico General registra: enfermería recibe 403")
    void soloElMedicoGeneralRegistra() throws Exception {
        mvc.perform(peticionAlta("4444444444444", RolNombre.ENFERMERIA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Dos internos con el mismo DPI se rechazan con 409")
    void elDpiNoSeRepite() throws Exception {
        registrarInterno("5555555555555");

        mvc.perform(peticionAlta("5555555555555", RolNombre.MEDICO_GENERAL))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Un DPI que no tiene 13 dígitos se rechaza con el error en su campo")
    void elDpiSeValida() throws Exception {
        mvc.perform(peticionAlta("123", RolNombre.MEDICO_GENERAL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.dpi").isNotEmpty());
    }

    @Test
    @DisplayName("El Médico General llena la ficha y lo guardado se vuelve a leer")
    void laFichaSeEscribeYSeLee() throws Exception {
        Long id = registrarInterno("6666666666666");
        String token = tokenDe(RolNombre.MEDICO_GENERAL);

        String ficha = """
                {"tipoSangre": "O+",
                 "alergias": "Penicilina",
                 "motivoIngreso": "Deterioro cognitivo",
                 "psicopatologias": [
                   {"nombre": "Demencia senil", "observaciones": "Progresiva"},
                   {"nombre": "Depresión mayor"}],
                 "medicamentosHabituales": [
                   {"nombre": "Donepezilo", "dosis": "10 mg", "frecuencia": "Cada 24 horas"}]}
                """;

        mvc.perform(put("/api/internos/{id}/ficha-medica", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ficha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.psicopatologias.length()").value(2));

        mvc.perform(get("/api/internos/{id}/ficha-medica", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoSangre").value("O+"))
                .andExpect(jsonPath("$.psicopatologias.length()").value(2))
                .andExpect(jsonPath("$.medicamentosHabituales[0].dosis").value("10 mg"));
    }

    /**
     * Las listas se reemplazan enteras. Si en lugar de eso se acumularan, cada
     * corrección del médico dejaría el padecimiento anterior en la ficha.
     */
    @Test
    @DisplayName("Guardar la ficha de nuevo reemplaza las listas, no las acumula")
    void laFichaReemplazaSusListas() throws Exception {
        Long id = registrarInterno("7777777777777");
        String token = tokenDe(RolNombre.MEDICO_GENERAL);

        guardarFicha(id, token, """
                {"psicopatologias": [{"nombre": "Diagnóstico equivocado"}]}
                """);
        guardarFicha(id, token, """
                {"psicopatologias": [{"nombre": "Diagnóstico corregido"}]}
                """);

        mvc.perform(get("/api/internos/{id}/ficha-medica", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(jsonPath("$.psicopatologias.length()").value(1))
                .andExpect(jsonPath("$.psicopatologias[0].nombre").value("Diagnóstico corregido"));
    }

    @Test
    @DisplayName("La búsqueda filtra la lista por nombre")
    void laBusquedaFiltra() throws Exception {
        registrarInterno("8888888888888");

        mvc.perform(get("/api/internos").param("busqueda", "rosa elena")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ENFERMERIA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isNotEmpty());
    }

    // ---- apoyo ----

    private void guardarFicha(Long id, String token, String cuerpo) throws Exception {
        mvc.perform(put("/api/internos/{id}/ficha-medica", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isOk());
    }

    private Long registrarInterno(String dpi) throws Exception {
        String cuerpo = mvc.perform(peticionAlta(dpi, RolNombre.MEDICO_GENERAL))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    private RequestBuilder peticionAlta(String dpi, RolNombre rol) throws Exception {
        String cuerpo = """
                {"nombreCompleto": "Rosa Elena Morales",
                 "dpi": "%s",
                 "fechaNacimiento": "1941-03-17",
                 "fechaIngreso": "2024-08-01",
                 "cuotaMensual": 850.00,
                 "familiarResponsable": {
                   "nombreCompleto": "Carmen Morales",
                   "parentesco": "Hija",
                   "telefono": "50212345678"}}
                """.formatted(dpi);

        return post("/api/internos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
    }

    /** Un usuario por rol, rehecho en cada llamada como en AutorizacionTest. */
    private String tokenDe(RolNombre rol) throws Exception {
        String correo = rol.name().toLowerCase(Locale.ROOT) + ".internos@asilo.gt";

        Usuario usuario = usuarioRepository.findByCorreo(correo).orElseGet(Usuario::new);
        usuario.setNombreCompleto("Persona de prueba");
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD));
        usuario.setRol(rolRepository.findByNombre(rol).orElseThrow());
        usuario.setActivo(true);
        usuario.setFechaInvalidacionSesiones(null);
        usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);

        String respuesta = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"%s\", \"password\": \"%s\"}".formatted(correo, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(respuesta, "$.token");
    }
}
