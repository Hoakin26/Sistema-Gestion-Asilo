package app.cabezadealgodon.com.gestionasilobackend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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

import tools.jackson.databind.ObjectMapper;

import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.RolRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;

/**
 * YS-59. Los campos obligatorios del perfil del interno y sus reglas, que es
 * el criterio "Validar campos obligatorios" de HU-01.
 *
 * <p>
 * Cada caso comprueba no solo que la petición se rechace, sino que el error
 * venga <b>en el campo correcto</b>. Un 400 con el error en otro lado es tan
 * inútil para el usuario como no recibir ninguno: el formulario no sabría
 * junto a qué campo mostrarlo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Validación del perfil del interno (YS-59)")
class InternoValidacionTest extends PruebaConBaseDeDatos {

    private static final String PASSWORD = "Prueba1234";
    private static final String CORREO = "medico.validacion@asilo.gt";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Contador para que cada alta válida use un DPI distinto. */
    private static int siguienteDpi = 0;

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "nombreCompleto", "dpi", "fechaNacimiento", "fechaIngreso", "familiarResponsable" })
    @DisplayName("Falta un campo obligatorio del interno")
    void faltaUnCampoDelInterno(String campo) throws Exception {
        Map<String, Object> cuerpo = internoValido();
        cuerpo.remove(campo);

        alta(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['%s']".formatted(campo)).isNotEmpty());
    }

    /**
     * Los errores del familiar llegan con la ruta completa
     * ({@code familiarResponsable.telefono}). Es lo que permite al formulario
     * distinguir el nombre del familiar del nombre del interno, que se llaman
     * igual.
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "nombreCompleto", "parentesco", "telefono" })
    @DisplayName("Falta un campo obligatorio del familiar responsable")
    @SuppressWarnings("unchecked")
    void faltaUnCampoDelFamiliar(String campo) throws Exception {
        Map<String, Object> cuerpo = internoValido();
        ((Map<String, Object>) cuerpo.get("familiarResponsable")).remove(campo);

        alta(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['familiarResponsable.%s']".formatted(campo)).isNotEmpty());
    }

    @Test
    @DisplayName("Un nombre hecho solo de espacios cuenta como vacío")
    void unNombreDeEspaciosNoPasa() throws Exception {
        Map<String, Object> cuerpo = internoValido();
        cuerpo.put("nombreCompleto", "    ");

        alta(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombreCompleto").isNotEmpty());
    }

    /**
     * La decisión de YS-59: el Médico General registra al paciente aunque no
     * conozca la cuota, que es un dato financiero fuera de su competencia.
     */
    @Test
    @DisplayName("La cuota mensual es opcional al registrar")
    void laCuotaEsOpcional() throws Exception {
        Map<String, Object> cuerpo = internoValido();
        cuerpo.remove("cuotaMensual");

        alta(cuerpo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuotaMensual").doesNotExist());
    }

    @Test
    @DisplayName("Si viene la cuota, no puede ser negativa")
    void laCuotaNoPuedeSerNegativa() throws Exception {
        Map<String, Object> cuerpo = internoValido();
        cuerpo.put("cuotaMensual", -100);

        alta(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cuotaMensual").isNotEmpty());
    }

    @Test
    @DisplayName("La fecha de ingreso no puede ser anterior al nacimiento")
    void elIngresoNoPuedeSerAnteriorAlNacimiento() throws Exception {
        Map<String, Object> cuerpo = internoValido();
        // El error típico de carga a mano: las dos fechas invertidas.
        cuerpo.put("fechaNacimiento", "2024-08-01");
        cuerpo.put("fechaIngreso", "1941-03-17");

        alta(cuerpo)
                .andExpect(status().isBadRequest())
                // En fechaIngreso y no en el objeto: es el campo a corregir.
                .andExpect(jsonPath("$.errores.fechaIngreso").isNotEmpty());
    }

    @Test
    @DisplayName("La fecha de nacimiento no puede estar en el futuro")
    void elNacimientoNoPuedeSerFuturo() throws Exception {
        Map<String, Object> cuerpo = internoValido();
        cuerpo.put("fechaNacimiento", "2999-01-01");

        alta(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fechaNacimiento").isNotEmpty());
    }

    /**
     * La validación también entra en las listas de la ficha. Un medicamento
     * sin dosis no le sirve a quien tiene que administrarlo, y el error tiene
     * que señalar cuál de todos los medicamentos de la lista está incompleto.
     */
    @Test
    @DisplayName("Un medicamento de la ficha sin dosis se rechaza señalando cuál")
    void unMedicamentoSinDosisSeRechaza() throws Exception {
        Long id = idDe(alta(internoValido()).andExpect(status().isCreated()));

        Map<String, Object> ficha = Map.of("medicamentosHabituales", List.of(
                Map.of("nombre", "Donepezilo", "dosis", "10 mg", "frecuencia", "Cada 24 horas"),
                Map.of("nombre", "Memantina", "frecuencia", "Cada 12 horas")));

        mvc.perform(put("/api/internos/{id}/ficha-medica", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(ficha)))
                .andExpect(status().isBadRequest())
                // El segundo, no el primero: el índice tiene que ser el correcto.
                .andExpect(jsonPath("$.errores['medicamentosHabituales[1].dosis']").isNotEmpty());
    }

    // ---- apoyo ----

    private Map<String, Object> internoValido() {
        Map<String, Object> familiar = new LinkedHashMap<>();
        familiar.put("nombreCompleto", "Carmen Morales");
        familiar.put("parentesco", "Hija");
        familiar.put("telefono", "50212345678");

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("nombreCompleto", "Rosa Elena Morales");
        cuerpo.put("dpi", "%013d".formatted(9_000_000_000_000L + siguienteDpi++));
        cuerpo.put("fechaNacimiento", "1941-03-17");
        cuerpo.put("fechaIngreso", "2024-08-01");
        cuerpo.put("cuotaMensual", 850.00);
        cuerpo.put("familiarResponsable", familiar);
        return cuerpo;
    }

    private ResultActions alta(Map<String, Object> cuerpo) throws Exception {
        return mvc.perform(post("/api/internos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo)));
    }

    private Long idDe(ResultActions resultado) throws Exception {
        String cuerpo = resultado.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    private String token() throws Exception {
        Usuario usuario = usuarioRepository.findByCorreo(CORREO).orElseGet(Usuario::new);
        usuario.setNombreCompleto("Médico de prueba");
        usuario.setCorreo(CORREO);
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD));
        usuario.setRol(rolRepository.findByNombre(RolNombre.MEDICO_GENERAL).orElseThrow());
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
