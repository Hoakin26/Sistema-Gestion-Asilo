package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.EspecialidadRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.RolRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * YS-93 (YS-167). Los especialistas de la fundación: su registro, las reglas
 * de su horario, sus horas libres y que solo la Fundación los administre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Especialistas y su horario (YS-93)")
class EspecialistaTest extends PruebaConBaseDeDatos {

    private static final String PASSWORD = "Prueba1234";

    /** Cada prueba usa cuentas nuevas: una cuenta solo puede tener un perfil. */
    private static final AtomicLong SIGUIENTE = new AtomicLong();

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
    @DisplayName("La Fundación registra un especialista con su horario, ordenado de lunes a domingo")
    void registraUnEspecialista() throws Exception {
        Usuario medico = nuevoUsuario(RolNombre.MEDICO_ESPECIALISTA);

        registrar(perfil(medico.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medico.id").value(medico.getId()))
                .andExpect(jsonPath("$.duracionCitaMinutos").value(30))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.horarios.length()").value(7))
                .andExpect(jsonPath("$.horarios[0].dia").value("MONDAY"))
                .andExpect(jsonPath("$.horarios[6].dia").value("SUNDAY"));
    }

    @Test
    @DisplayName("Solo una cuenta de Médico Especialista puede tener perfil de especialista")
    void laCuentaTieneQueSerDeEspecialista() throws Exception {
        Usuario enfermero = nuevoUsuario(RolNombre.ENFERMERIA);

        registrar(perfil(enfermero.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.usuarioId").isNotEmpty());
    }

    @Test
    @DisplayName("Una cuenta no puede tener dos perfiles: el segundo es un 409")
    void noHayPerfilesDuplicados() throws Exception {
        Usuario medico = nuevoUsuario(RolNombre.MEDICO_ESPECIALISTA);
        registrar(perfil(medico.getId())).andExpect(status().isCreated());

        registrar(perfil(medico.getId())).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Un horario cuyo fin no es posterior al inicio se rechaza en su fila")
    void elFinTieneQueSerPosterior() throws Exception {
        Map<String, Object> cuerpo = perfil(nuevoUsuario(RolNombre.MEDICO_ESPECIALISTA).getId());
        cuerpo.put("horarios", List.of(horario("MONDAY", "12:00", "08:00")));

        registrar(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['horarios[0].horaFin']").isNotEmpty());
    }

    @Test
    @DisplayName("Un horario que no alcanza para una sola cita se rechaza")
    void elHorarioTieneQueAlcanzar() throws Exception {
        Map<String, Object> cuerpo = perfil(nuevoUsuario(RolNombre.MEDICO_ESPECIALISTA).getId());
        cuerpo.put("horarios", List.of(horario("MONDAY", "08:00", "08:20")));

        registrar(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['horarios[0].horaFin']").isNotEmpty());
    }

    @Test
    @DisplayName("El mismo día dos veces en el horario se rechaza")
    void noHayDiasRepetidos() throws Exception {
        Map<String, Object> cuerpo = perfil(nuevoUsuario(RolNombre.MEDICO_ESPECIALISTA).getId());
        cuerpo.put("horarios", List.of(horario("MONDAY", "08:00", "10:00"), horario("MONDAY", "14:00", "16:00")));

        registrar(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['horarios[1].dia']").isNotEmpty());
    }

    @Test
    @DisplayName("Sin ningún día de atención no hay perfil")
    void hayQueAtenderAlgunDia() throws Exception {
        Map<String, Object> cuerpo = perfil(nuevoUsuario(RolNombre.MEDICO_ESPECIALISTA).getId());
        cuerpo.put("horarios", List.of());

        registrar(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.horarios").isNotEmpty());
    }

    @Test
    @DisplayName("Las cuentas disponibles para registrar dejan de serlo al tener perfil")
    void lasCuentasDisponiblesSeActualizan() throws Exception {
        Usuario medico = nuevoUsuario(RolNombre.MEDICO_ESPECIALISTA);
        assertThat(idsDisponibles()).contains(medico.getId().intValue());

        registrar(perfil(medico.getId())).andExpect(status().isCreated());

        assertThat(idsDisponibles()).doesNotContain(medico.getId().intValue());
    }

    @Test
    @DisplayName("Un especialista desactivado ya no se ofrece para su especialidad")
    void elInactivoNoSeOfrece() throws Exception {
        Map<String, Object> cuerpo = perfil(nuevoUsuario(RolNombre.MEDICO_ESPECIALISTA).getId());
        Long id = idDe(registrar(cuerpo).andExpect(status().isCreated()));
        Long especialidadId = ((Number) cuerpo.get("especialidadId")).longValue();
        assertThat(idsDeEspecialidad(especialidadId)).contains(id.intValue());

        cuerpo.put("activo", false);
        mvc.perform(put("/api/especialistas/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_FUNDACION))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        assertThat(idsDeEspecialidad(especialidadId)).doesNotContain(id.intValue());
    }

    /**
     * Editar sin cambiar los días respondía 409: Hibernate insertaba los
     * horarios nuevos antes de borrar los viejos y chocaba contra la
     * restricción (especialista, día). Esta prueba y la anterior lo cubren.
     */
    @Test
    @DisplayName("Editar el horario quita días, cambia horas y cambia la duración de la cita")
    void editaElHorario() throws Exception {
        Map<String, Object> cuerpo = perfil(nuevoUsuario(RolNombre.MEDICO_ESPECIALISTA).getId());
        Long id = idDe(registrar(cuerpo).andExpect(status().isCreated()));

        cuerpo.put("duracionCitaMinutos", 60);
        cuerpo.put("horarios", List.of(horario("MONDAY", "09:00", "11:00")));
        mvc.perform(put("/api/especialistas/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_FUNDACION))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duracionCitaMinutos").value(60))
                .andExpect(jsonPath("$.horarios.length()").value(1))
                .andExpect(jsonPath("$.horarios[0].horaInicio").value("09:00:00"));
    }

    /** De 8:00 a 12:00 con citas de 30 minutos: ocho franjas. */
    @Test
    @DisplayName("Las horas libres de un día salen del horario, de una duración en una duración")
    void lasHorasLibresSalenDelHorario() throws Exception {
        Long id = idDe(registrar(perfil(nuevoUsuario(RolNombre.MEDICO_ESPECIALISTA).getId())));

        List<String> horas = disponibilidad(id, LocalDate.now().plusDays(7));

        assertThat(horas).hasSize(8);
        assertThat(horas.get(0)).startsWith("08:00");
        assertThat(horas.get(7)).startsWith("11:30");
    }

    @Test
    @DisplayName("Un día que ya pasó no tiene horas libres")
    void elPasadoNoTieneHoras() throws Exception {
        Long id = idDe(registrar(perfil(nuevoUsuario(RolNombre.MEDICO_ESPECIALISTA).getId())));

        assertThat(disponibilidad(id, LocalDate.now().minusDays(1))).isEmpty();
    }

    @Test
    @DisplayName("Consultar las horas libres sin fecha es un 400 con el formato de la API")
    void laFechaEsObligatoria() throws Exception {
        Long id = idDe(registrar(perfil(nuevoUsuario(RolNombre.MEDICO_ESPECIALISTA).getId())));

        mvc.perform(get("/api/especialistas/{id}/disponibilidad", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_FUNDACION)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Falta el parámetro 'fecha'"));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = "ADMINISTRADOR_FUNDACION", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Los especialistas los administra solo la Fundación")
    void soloLaFundacion(RolNombre rol) throws Exception {
        String token = tokenDe(rol);

        mvc.perform(get("/api/especialistas").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/especialistas")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(perfil(1L))))
                .andExpect(status().isForbidden());
    }

    // ---- apoyo ----

    /** Un perfil válido: Cardiología, citas de 30 minutos, toda la semana de 8:00 a 12:00. */
    private Map<String, Object> perfil(Long usuarioId) {
        List<Map<String, Object>> horarios = new ArrayList<>();
        for (DayOfWeek dia : DayOfWeek.values()) {
            horarios.add(horario(dia.name(), "08:00", "12:00"));
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("usuarioId", usuarioId);
        cuerpo.put("especialidadId", especialidadRepository.findByActivaTrueOrderByNombreAsc().get(0).getId());
        cuerpo.put("duracionCitaMinutos", 30);
        cuerpo.put("horarios", horarios);
        return cuerpo;
    }

    private static Map<String, Object> horario(String dia, String inicio, String fin) {
        Map<String, Object> horario = new LinkedHashMap<>();
        horario.put("dia", dia);
        horario.put("horaInicio", inicio);
        horario.put("horaFin", fin);
        return horario;
    }

    private ResultActions registrar(Map<String, Object> cuerpo) throws Exception {
        return mvc.perform(post("/api/especialistas")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_FUNDACION))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo)));
    }

    private static Long idDe(ResultActions resultado) throws Exception {
        String respuesta = resultado.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private List<Integer> idsDisponibles() throws Exception {
        return JsonPath.read(obtener("/api/especialistas/usuarios-disponibles"), "$[*].id");
    }

    private List<Integer> idsDeEspecialidad(Long especialidadId) throws Exception {
        return JsonPath.read(obtener("/api/especialistas?especialidadId=" + especialidadId), "$[*].id");
    }

    private List<String> disponibilidad(Long id, LocalDate fecha) throws Exception {
        return JsonPath.read(obtener("/api/especialistas/" + id + "/disponibilidad?fecha=" + fecha), "$");
    }

    private String obtener(String ruta) throws Exception {
        return mvc.perform(get(ruta)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_FUNDACION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** Una cuenta nueva del rol, con correo propio. */
    private Usuario nuevoUsuario(RolNombre rol) {
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto("Especialista de prueba " + SIGUIENTE.incrementAndGet());
        usuario.setCorreo(rol.name().toLowerCase(Locale.ROOT) + ".perfil" + SIGUIENTE.get() + "@asilo.gt");
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD));
        usuario.setRol(rolRepository.findByNombre(rol).orElseThrow());
        usuario.setActivo(true);
        usuario.setDebeCambiarPassword(false);
        return usuarioRepository.save(usuario);
    }

    private String tokenDe(RolNombre rol) throws Exception {
        String correo = rol.name().toLowerCase(Locale.ROOT) + ".especialistas@asilo.gt";
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
