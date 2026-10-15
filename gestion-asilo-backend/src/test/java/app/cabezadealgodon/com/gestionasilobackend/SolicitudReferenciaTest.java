package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.EspecialidadRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.RolRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * YS-86. La solicitud de referencia (HU-03, RF-03): quién la crea, con qué
 * datos, y que el número correlativo no se repita nunca.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Solicitud de referencia (YS-86)")
class SolicitudReferenciaTest extends PruebaConBaseDeDatos {

    private static final String PASSWORD = "Prueba1234";

    /** DPIs propios de esta clase: la base es compartida por toda la ejecución. */
    private static final AtomicLong SIGUIENTE_DPI = new AtomicLong(6_200_000_000_000L);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("El Médico General la crea: nace pendiente de asignación, con su número y a su nombre")
    void nacePendienteYConNumero() throws Exception {
        Usuario medico = usuarioDe(RolNombre.MEDICO_GENERAL, "");
        Usuario enfermero = usuarioDe(RolNombre.ENFERMERIA, "");

        crear(solicitudValida(), tokenDe(medico))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_ASIGNACION"))
                .andExpect(jsonPath("$.numero").value(org.hamcrest.Matchers.matchesPattern(
                        "REF-" + Year.now().getValue() + "-\\d{4}")))
                .andExpect(jsonPath("$.medicoGeneral.id").value(medico.getId()))
                .andExpect(jsonPath("$.enfermeroAcompanante.id").value(enfermero.getId()))
                .andExpect(jsonPath("$.motivo").value("Pérdida de memoria reciente que avanza"));
    }

    @ParameterizedTest(name = "falta {0}")
    @ValueSource(strings = { "internoId", "especialidadId", "enfermeroAcompananteId", "motivo" })
    @DisplayName("Cada campo obligatorio que falta se rechaza con el error en su campo")
    void cadaCampoEsObligatorio(String campo) throws Exception {
        Map<String, Object> cuerpo = solicitudValida();
        cuerpo.remove(campo);

        crear(cuerpo, tokenDe(usuarioDe(RolNombre.MEDICO_GENERAL, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores." + campo).isNotEmpty());
    }

    @Test
    @DisplayName("Un motivo hecho solo de espacios cuenta como vacío")
    void elMotivoEnBlancoNoSirve() throws Exception {
        Map<String, Object> cuerpo = solicitudValida();
        cuerpo.put("motivo", "    ");

        crear(cuerpo, tokenDe(usuarioDe(RolNombre.MEDICO_GENERAL, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivo").isNotEmpty());
    }

    @Test
    @DisplayName("Dos solicitudes seguidas reciben números consecutivos")
    void losNumerosSonConsecutivos() throws Exception {
        String token = tokenDe(usuarioDe(RolNombre.MEDICO_GENERAL, ""));

        int primero = correlativoDe(crear(solicitudValida(), token));
        int segundo = correlativoDe(crear(solicitudValida(), token));

        assertThat(segundo).isEqualTo(primero + 1);
    }

    /**
     * El caso que el correlativo tiene que resistir: varias solicitudes en el
     * mismo instante. Calcular "el máximo más uno" pasaría la prueba anterior
     * y fallaría esta, porque todas leerían el mismo máximo.
     */
    @Test
    @DisplayName("Cinco solicitudes creadas a la vez reciben cinco números distintos")
    void lasSimultaneasNoRepitenNumero() throws Exception {
        String token = tokenDe(usuarioDe(RolNombre.MEDICO_GENERAL, ""));
        String cuerpo = json.writeValueAsString(solicitudValida());

        int simultaneas = 5;
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(simultaneas);
        try {
            List<Future<String>> respuestas = new ArrayList<>();
            for (int i = 0; i < simultaneas; i++) {
                Callable<String> tarea = () -> {
                    largada.await();
                    String respuesta = mvc.perform(post("/api/solicitudes-referencia")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                            .andExpect(status().isCreated())
                            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
                    return JsonPath.read(respuesta, "$.numero");
                };
                respuestas.add(hilos.submit(tarea));
            }

            largada.countDown();

            var numeros = new HashSet<String>();
            for (Future<String> respuesta : respuestas) {
                numeros.add(respuesta.get());
            }
            assertThat(numeros).hasSize(simultaneas);
        } finally {
            hilos.shutdownNow();
        }
    }

    /**
     * RF-03: la crea el médico general. Enfermería es la acompañante, no quien
     * refiere; antes del 08/10 el plan decía lo contrario y por eso esta
     * prueba nombra a enfermería, aunque el barrido de abajo ya la incluye.
     */
    @Test
    @DisplayName("Enfermería intenta crear una referencia y recibe 403")
    void enfermeriaNoCrea() throws Exception {
        crear(solicitudValida(), tokenDe(usuarioDe(RolNombre.ENFERMERIA, "")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = "MEDICO_GENERAL", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Ningún rol distinto del Médico General crea solicitudes")
    void soloElMedicoGeneralCrea(RolNombre rol) throws Exception {
        crear(solicitudValida(), tokenDe(usuarioDe(rol, "")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Asignar como acompañante a alguien que no es de enfermería se rechaza")
    void elAcompananteTieneQueSerDeEnfermeria() throws Exception {
        Map<String, Object> cuerpo = solicitudValida();
        cuerpo.put("enfermeroAcompananteId", usuarioDe(RolNombre.FARMACIA, "").getId());

        crear(cuerpo, tokenDe(usuarioDe(RolNombre.MEDICO_GENERAL, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.enfermeroAcompananteId").isNotEmpty());
    }

    @Test
    @DisplayName("Un enfermero con la cuenta desactivada no puede ser acompañante")
    void elAcompananteTieneQueEstarActivo() throws Exception {
        Usuario inactivo = usuarioDe(RolNombre.ENFERMERIA, ".inactivo");
        inactivo.setActivo(false);
        usuarioRepository.save(inactivo);

        Map<String, Object> cuerpo = solicitudValida();
        cuerpo.put("enfermeroAcompananteId", inactivo.getId());

        crear(cuerpo, tokenDe(usuarioDe(RolNombre.MEDICO_GENERAL, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.enfermeroAcompananteId").isNotEmpty());
    }

    @Test
    @DisplayName("Un interno o una especialidad que no existen se señalan en su campo")
    void losDatosInexistentesSeSenalan() throws Exception {
        String token = tokenDe(usuarioDe(RolNombre.MEDICO_GENERAL, ""));

        Map<String, Object> sinInterno = solicitudValida();
        sinInterno.put("internoId", 999_999);
        crear(sinInterno, token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.internoId").isNotEmpty());

        Map<String, Object> sinEspecialidad = solicitudValida();
        sinEspecialidad.put("especialidadId", 999_999);
        crear(sinEspecialidad, token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.especialidadId").isNotEmpty());
    }

    @Test
    @DisplayName("El catálogo de especialidades llega sembrado y ordenado")
    void lasEspecialidadesEstanSembradas() throws Exception {
        mvc.perform(get("/api/especialidades")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(usuarioDe(RolNombre.ADMINISTRADOR_FUNDACION, ""))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Cardiología"))
                .andExpect(jsonPath("$[?(@.nombre == 'Geriatría')]").isNotEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = { "MEDICO_GENERAL", "ADMINISTRADOR_FUNDACION" },
            mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Las especialidades las consultan solo el Médico General y la Fundación")
    void lasEspecialidadesSonDeLaReferencia(RolNombre rol) throws Exception {
        mvc.perform(get("/api/especialidades")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(usuarioDe(rol, ""))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("La lista de acompañantes trae solo enfermeros activos, sin su correo")
    void losAcompanantesSonEnfermerosActivos() throws Exception {
        Usuario enfermero = usuarioDe(RolNombre.ENFERMERIA, "");
        Usuario farmacia = usuarioDe(RolNombre.FARMACIA, "");

        mvc.perform(get("/api/enfermeros")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(usuarioDe(RolNombre.MEDICO_GENERAL, ""))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(enfermero.getId())).isNotEmpty())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(farmacia.getId())).isEmpty())
                .andExpect(jsonPath("$[0].correo").doesNotExist());
    }

    @Test
    @DisplayName("La lista de acompañantes es solo del Médico General")
    void losAcompanantesSonDelMedicoGeneral() throws Exception {
        mvc.perform(get("/api/enfermeros")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(usuarioDe(RolNombre.ADMINISTRADOR_ASILO, ""))))
                .andExpect(status().isForbidden());
    }

    // ---- apoyo ----

    /**
     * Una solicitud con todos los datos válidos. Registra un interno nuevo
     * cada vez, para que ninguna prueba dependa de lo que dejó otra.
     */
    private Map<String, Object> solicitudValida() throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("internoId", registrarInterno());
        cuerpo.put("especialidadId", especialidadRepository.findByActivaTrueOrderByNombreAsc().get(0).getId());
        cuerpo.put("enfermeroAcompananteId", usuarioDe(RolNombre.ENFERMERIA, "").getId());
        cuerpo.put("motivo", "Pérdida de memoria reciente que avanza");
        return cuerpo;
    }

    private ResultActions crear(Map<String, Object> cuerpo, String token) throws Exception {
        return mvc.perform(post("/api/solicitudes-referencia")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo)));
    }

    /** El 0042 de "REF-2026-0042". */
    private static int correlativoDe(ResultActions resultado) throws Exception {
        String respuesta = resultado.andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String numero = JsonPath.read(respuesta, "$.numero");
        return Integer.parseInt(numero.substring(numero.lastIndexOf('-') + 1));
    }

    private Long registrarInterno() throws Exception {
        String cuerpo = """
                {"nombreCompleto": "Interno de referencia",
                 "dpi": "%d",
                 "fechaNacimiento": "1940-01-15",
                 "fechaIngreso": "2023-06-01",
                 "familiarResponsable": {
                   "nombreCompleto": "Familiar de prueba",
                   "parentesco": "Hija",
                   "telefono": "5555 0000"}}
                """.formatted(SIGUIENTE_DPI.getAndIncrement());

        String respuesta = mvc.perform(post("/api/internos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(usuarioDe(RolNombre.MEDICO_GENERAL, "")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    /** Un usuario por rol (y variante), rehecho activo en cada llamada. */
    private Usuario usuarioDe(RolNombre rol, String variante) {
        String correo = rol.name().toLowerCase(Locale.ROOT) + variante + ".referencias@asilo.gt";

        Usuario usuario = usuarioRepository.findByCorreo(correo).orElseGet(Usuario::new);
        usuario.setNombreCompleto("Persona de prueba " + rol.name().toLowerCase(Locale.ROOT));
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD));
        usuario.setRol(rolRepository.findByNombre(rol).orElseThrow());
        usuario.setActivo(true);
        usuario.setFechaInvalidacionSesiones(null);
        usuario.setDebeCambiarPassword(false);
        return usuarioRepository.save(usuario);
    }

    private String tokenDe(Usuario usuario) throws Exception {
        String respuesta = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"%s\", \"password\": \"%s\"}".formatted(usuario.getCorreo(), PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(respuesta, "$.token");
    }
}
