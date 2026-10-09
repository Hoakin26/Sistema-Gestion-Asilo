package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

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

import com.jayway.jsonpath.JsonPath;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoSolicitud;
import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.SolicitudReferencia;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.EspecialidadRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.RolRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.SolicitudReferenciaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;

/**
 * YS-89. El listado de solicitudes (HU-05, RF-05): el filtro por estado, el
 * orden y quién puede consultarlo.
 *
 * <p>
 * La base es compartida con las demás clases de prueba, así que estas
 * pruebas no cuentan filas: buscan las solicitudes que ellas mismas crearon
 * dentro de la respuesta.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Listado de solicitudes de referencia (YS-89)")
class SolicitudListadoTest extends PruebaConBaseDeDatos {

    private static final String PASSWORD = "Prueba1234";
    private static final AtomicLong SIGUIENTE_DPI = new AtomicLong(6_400_000_000_000L);

    /** Lo bastante para traer todas las de la base de pruebas en una página. */
    private static final String TODAS = "1000";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Autowired
    private SolicitudReferenciaRepository solicitudRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Filtrar por estado devuelve solo las de ese estado")
    void filtraPorEstado() throws Exception {
        Long pendiente = crearSolicitud();
        Long cancelada = crearSolicitud();
        cambiarEstado(cancelada, EstadoSolicitud.CANCELADA);

        String respuesta = listar("estado=PENDIENTE_ASIGNACION&size=" + TODAS, RolNombre.ADMINISTRADOR_FUNDACION);
        List<Integer> ids = JsonPath.read(respuesta, "$.content[*].id");
        List<String> estados = JsonPath.read(respuesta, "$.content[*].estado");

        assertThat(ids).contains(pendiente.intValue()).doesNotContain(cancelada.intValue());
        assertThat(estados).containsOnly("PENDIENTE_ASIGNACION");
    }

    @Test
    @DisplayName("Sin estado devuelve todas, de cualquier estado")
    void sinEstadoDevuelveTodas() throws Exception {
        Long pendiente = crearSolicitud();
        Long cancelada = crearSolicitud();
        cambiarEstado(cancelada, EstadoSolicitud.CANCELADA);

        List<Integer> ids = JsonPath.read(listar("size=" + TODAS, RolNombre.MEDICO_GENERAL), "$.content[*].id");

        assertThat(ids).contains(pendiente.intValue(), cancelada.intValue());
    }

    /**
     * La Fundación ve primero la que más tiempo lleva esperando: es la más
     * urgente. El médico puede pedir el orden inverso para ver primero las
     * que acaba de crear.
     */
    @Test
    @DisplayName("Por defecto van de la más antigua a la más reciente, y el orden se puede invertir")
    void ordenPorAntiguedad() throws Exception {
        Long primera = crearSolicitud();
        Long segunda = crearSolicitud();

        List<Integer> ascendente = JsonPath.read(listar("size=" + TODAS, RolNombre.ADMINISTRADOR_FUNDACION),
                "$.content[*].id");
        List<Integer> descendente = JsonPath.read(
                listar("size=" + TODAS + "&sort=fechaCreacion,desc", RolNombre.MEDICO_GENERAL), "$.content[*].id");

        assertThat(ascendente.indexOf(primera.intValue())).isLessThan(ascendente.indexOf(segunda.intValue()));
        assertThat(descendente.indexOf(segunda.intValue())).isLessThan(descendente.indexOf(primera.intValue()));
    }

    /** Lo que pide HU-05 de cada fila: el paciente, la especialidad y el motivo. */
    @Test
    @DisplayName("Cada solicitud trae su número, el paciente, la especialidad, el motivo y el acompañante")
    void cadaFilaTraeLoQuePideHu05() throws Exception {
        Long id = crearSolicitud();

        String respuesta = listar("size=" + TODAS, RolNombre.ADMINISTRADOR_FUNDACION);
        String fila = "$.content[?(@.id == %d)]".formatted(id);

        assertThat((List<String>) JsonPath.read(respuesta, fila + ".numero")).hasSize(1);
        assertThat((List<String>) JsonPath.read(respuesta, fila + ".interno.nombreCompleto"))
                .containsExactly("Interno de la bandeja");
        assertThat((List<String>) JsonPath.read(respuesta, fila + ".especialidad.nombre")).isNotEmpty();
        assertThat((List<String>) JsonPath.read(respuesta, fila + ".motivo"))
                .containsExactly("Evaluación por caídas repetidas");
        assertThat((List<String>) JsonPath.read(respuesta, fila + ".enfermeroAcompanante.nombreCompleto"))
                .isNotEmpty();
    }

    /**
     * YS-166. Se busca como se escribe en la tableta: el número tal cual, el
     * nombre en minúsculas y la especialidad sin tildes.
     */
    @Test
    @DisplayName("La búsqueda encuentra por número, por interno y por especialidad sin tildes")
    void laBusquedaEncuentra() throws Exception {
        Long id = crearSolicitud();
        String numero = solicitudRepository.findById(id).orElseThrow().getNumero();

        for (String termino : List.of(numero, "interno de la BANDEJA", "cardiologia")) {
            assertThat(idsBuscando(termino)).as("buscando «%s»", termino).contains(id.intValue());
        }
        assertThat(idsBuscando("zzz-no-existe")).isEmpty();
    }

    @Test
    @DisplayName("Ordenar los internos por un campo que no existe también es un 400")
    void ordenInvalidoEnInternos() throws Exception {
        mvc.perform(get("/api/internos").param("sort", "campoInventado")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.MEDICO_GENERAL)))
                .andExpect(status().isBadRequest());
    }

    /* Con .param y no pegado a la URL: así los espacios viajan como espacios. */
    private List<Integer> idsBuscando(String termino) throws Exception {
        String respuesta = mvc.perform(get("/api/solicitudes-referencia")
                .param("busqueda", termino).param("size", TODAS)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_FUNDACION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(respuesta, "$.content[*].id");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = { "MEDICO_GENERAL", "ADMINISTRADOR_FUNDACION" },
            mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Los otros cinco roles reciben 403")
    void losDemasRolesNoConsultan(RolNombre rol) throws Exception {
        mvc.perform(get("/api/solicitudes-referencia")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol)))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = { "MEDICO_GENERAL", "ADMINISTRADOR_FUNDACION" })
    @DisplayName("El Médico General y la Fundación sí consultan")
    void quienesParticipanConsultan(RolNombre rol) throws Exception {
        mvc.perform(get("/api/solicitudes-referencia")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol)))
                .andExpect(status().isOk());
    }

    /**
     * 401 y no 403: es la distinción con que el frontend decide si la sesión
     * se cayó (y lo manda a iniciar sesión) o si solo le falta permiso.
     */
    @Test
    @DisplayName("Sin token responde 401, no 403")
    void sinTokenEs401() throws Exception {
        mvc.perform(get("/api/solicitudes-referencia")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un estado que no existe se rechaza con 400 y el formato de error de la API")
    void estadoInvalido() throws Exception {
        mvc.perform(get("/api/solicitudes-referencia").param("estado", "INVENTADO")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_FUNDACION)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El valor de 'estado' no es válido"));
    }

    @Test
    @DisplayName("Ordenar por un campo que no existe es un 400, no un error del servidor")
    void ordenInvalido() throws Exception {
        mvc.perform(get("/api/solicitudes-referencia").param("sort", "campoInventado")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_FUNDACION)))
                .andExpect(status().isBadRequest());
    }

    // ---- apoyo ----

    private String listar(String parametros, RolNombre rol) throws Exception {
        return mvc.perform(get("/api/solicitudes-referencia?" + parametros)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** Lleva la solicitud a otro estado por la entidad, que aplica el diagrama DE-01. */
    private void cambiarEstado(Long id, EstadoSolicitud estado) {
        SolicitudReferencia solicitud = solicitudRepository.findById(id).orElseThrow();
        solicitud.cambiarEstado(estado);
        solicitudRepository.save(solicitud);
    }

    private Long crearSolicitud() throws Exception {
        String cuerpo = """
                {"internoId": %d, "especialidadId": %d, "enfermeroAcompananteId": %d,
                 "motivo": "Evaluación por caídas repetidas"}
                """.formatted(
                registrarInterno(),
                especialidadRepository.findByActivaTrueOrderByNombreAsc().get(0).getId(),
                usuarioDe(RolNombre.ENFERMERIA).getId());

        String respuesta = mvc.perform(post("/api/solicitudes-referencia")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.MEDICO_GENERAL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private Long registrarInterno() throws Exception {
        String cuerpo = """
                {"nombreCompleto": "Interno de la bandeja",
                 "dpi": "%d",
                 "fechaNacimiento": "1939-11-30",
                 "fechaIngreso": "2021-03-10",
                 "familiarResponsable": {
                   "nombreCompleto": "Familiar de prueba",
                   "parentesco": "Hija",
                   "telefono": "5555 0000"}}
                """.formatted(SIGUIENTE_DPI.getAndIncrement());

        String respuesta = mvc.perform(post("/api/internos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.MEDICO_GENERAL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private Usuario usuarioDe(RolNombre rol) {
        String correo = rol.name().toLowerCase(Locale.ROOT) + ".bandeja@asilo.gt";

        Usuario usuario = usuarioRepository.findByCorreo(correo).orElseGet(Usuario::new);
        usuario.setNombreCompleto("Persona de prueba");
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD));
        usuario.setRol(rolRepository.findByNombre(rol).orElseThrow());
        usuario.setActivo(true);
        usuario.setFechaInvalidacionSesiones(null);
        usuario.setDebeCambiarPassword(false);
        return usuarioRepository.save(usuario);
    }

    private String tokenDe(RolNombre rol) throws Exception {
        Usuario usuario = usuarioDe(rol);
        String respuesta = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"%s\", \"password\": \"%s\"}".formatted(usuario.getCorreo(), PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(respuesta, "$.token");
    }
}
