package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

import app.cabezadealgodon.com.gestionasilobackend.models.Cita;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoSolicitud;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoVisita;
import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.SolicitudReferencia;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.models.VisitaMedica;
import app.cabezadealgodon.com.gestionasilobackend.repository.CitaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.EspecialidadRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.RolRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.SolicitudReferenciaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.VisitaMedicaRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * YS-101. El inicio de la visita médica (HU-07, RF-06, CU-003): que solo lo
 * haga el especialista asignado, solo sobre una cita programada, una sola vez
 * y desde el día de la cita; que la solicitud pase a En Atención y que la
 * visita copie la tarifa de la especialidad (YS-169).
 *
 * <p>
 * La asignación solo acepta horas futuras, así que las citas "de hoy" se
 * asignan una semana adelante y después se mueven en la base al día que la
 * prueba necesita.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Inicio de la visita médica (YS-101)")
class VisitaMedicaTest extends PruebaConBaseDeDatos {

    private static final String PASSWORD = "Prueba1234";
    private static final AtomicLong SIGUIENTE = new AtomicLong();
    private static final AtomicLong SIGUIENTE_DPI = new AtomicLong(6_700_000_000_000L);
    private static final LocalDate HOY = LocalDate.now();
    private static final LocalDate SEMANA_PROXIMA = HOY.plusDays(7);

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
    private CitaRepository citaRepository;

    @Autowired
    private SolicitudReferenciaRepository solicitudRepository;

    @Autowired
    private VisitaMedicaRepository visitaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("El especialista asignado inicia la visita: queda activa, con fecha y motivo, y la solicitud pasa a En Atención")
    void iniciaLaVisita() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Cita cita = citaEl(especialista, HOY.atTime(9, 0));

        iniciar(cita.getId(), especialista)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.estadoSolicitud").value("EN_ATENCION"))
                .andExpect(jsonPath("$.motivo").value("Control de seguimiento"))
                .andExpect(jsonPath("$.paciente.nombreCompleto").value("Interno con visita"))
                .andExpect(jsonPath("$.fechaInicio").isNotEmpty())
                .andExpect(jsonPath("$.especialista.nombreCompleto").value(especialista.usuario().getNombreCompleto()));

        assertThat(estadoDe(cita)).isEqualTo(EstadoSolicitud.EN_ATENCION);
    }

    @Test
    @DisplayName("Otro especialista no puede iniciar la visita: 403, y la solicitud sigue en Cita Programada")
    void otroEspecialistaNoLaInicia() throws Exception {
        Especialista asignado = nuevoEspecialista(0);
        Especialista otro = nuevoEspecialista(0);
        Cita cita = citaEl(asignado, HOY.atTime(9, 30));

        iniciar(cita.getId(), otro)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value(containsString("otro especialista")));

        assertThat(estadoDe(cita)).isEqualTo(EstadoSolicitud.CITA_PROGRAMADA);
        assertThat(visitaRepository.existsByCitaId(cita.getId())).isFalse();
    }

    @Test
    @DisplayName("Una cita no se inicia dos veces: la segunda responde 409")
    void noSeIniciaDosVeces() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Cita cita = citaEl(especialista, HOY.atTime(10, 0));

        iniciar(cita.getId(), especialista).andExpect(status().isCreated());
        iniciar(cita.getId(), especialista)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La visita de esta cita ya se inició"));
    }

    @Test
    @DisplayName("Una cita cuya solicitud no está en Cita Programada no se inicia: 409")
    void soloDesdeCitaProgramada() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Cita cita = citaEl(especialista, HOY.atTime(10, 30));
        SolicitudReferencia solicitud = solicitudRepository.findById(cita.getSolicitud().getId()).orElseThrow();
        solicitud.cambiarEstado(EstadoSolicitud.CANCELADA);
        solicitudRepository.save(solicitud);

        iniciar(cita.getId(), especialista).andExpect(status().isConflict());

        assertThat(visitaRepository.existsByCitaId(cita.getId())).isFalse();
    }

    /** Decidido el 09/10: nunca antes del día de la cita. */
    @Test
    @DisplayName("Una cita de la semana próxima todavía no se puede iniciar: 409 con el día desde el que se podrá")
    void noAntesDelDiaDeLaCita() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Cita cita = citaEl(especialista, SEMANA_PROXIMA.atTime(9, 0));

        iniciar(cita.getId(), especialista)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("se puede iniciar desde ese día")));

        assertThat(estadoDe(cita)).isEqualTo(EstadoSolicitud.CITA_PROGRAMADA);
    }

    /** La otra mitad de la misma decisión: una cita atrasada no queda bloqueada. */
    @Test
    @DisplayName("Una cita de ayer que no se atendió todavía se puede iniciar")
    void unaCitaAtrasadaSeInicia() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Cita cita = citaEl(especialista, HOY.minusDays(1).atTime(11, 0));

        iniciar(cita.getId(), especialista).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("La agenda trae las citas del día en orden, con su motivo, y la visita de la que ya se inició")
    void laAgendaDelDia() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Especialista otro = nuevoEspecialista(0);
        Cita tarde = citaEl(especialista, HOY.atTime(11, 0));
        Cita temprano = citaEl(especialista, HOY.atTime(8, 0));
        citaEl(especialista, HOY.plusDays(1).atTime(8, 0));
        citaEl(otro, HOY.atTime(9, 0));

        String visita = iniciar(temprano.getId(), especialista)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Number visitaId = JsonPath.read(visita, "$.id");

        // Sin fecha es la de hoy.
        mvc.perform(get("/api/citas/mi-agenda").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(temprano.getId()))
                .andExpect(jsonPath("$[0].visitaId").value(visitaId.longValue()))
                .andExpect(jsonPath("$[0].estado").value("EN_ATENCION"))
                .andExpect(jsonPath("$[0].motivo").value("Control de seguimiento"))
                .andExpect(jsonPath("$[1].id").value(tarde.getId()))
                .andExpect(jsonPath("$[1].visitaId").isEmpty());

        mvc.perform(get("/api/citas/mi-agenda").param("fecha", HOY.plusDays(1).toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Una cuenta de especialista sin perfil recibe 404 con qué hacer, no una agenda vacía")
    void sinPerfilNoHayAgenda() throws Exception {
        mvc.perform(get("/api/citas/mi-agenda")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.MEDICO_ESPECIALISTA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value(containsString("Médicos y disponibilidad")));
    }

    @Test
    @DisplayName("El especialista ve sus visitas y el detalle de cada una; otro especialista recibe 403")
    void susVisitas() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Especialista otro = nuevoEspecialista(0);
        Cita cita = citaEl(especialista, HOY.atTime(8, 30));
        String respuesta = iniciar(cita.getId(), especialista)
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long visita = ((Number) JsonPath.read(respuesta, "$.id")).longValue();

        mvc.perform(get("/api/visitas").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(visita));
        mvc.perform(get("/api/visitas").param("estado", "FINALIZADA")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(get("/api/visitas/{id}", visita).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroSolicitud").isNotEmpty());
        mvc.perform(get("/api/visitas/{id}", visita).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(otro)))
                .andExpect(status().isForbidden());
    }

    /**
     * RNF-03 incluido: el Administrador del Asilo está entre los seis, y la
     * visita es un dato clínico.
     */
    @ParameterizedTest
    @EnumSource(value = RolNombre.class, names = "MEDICO_ESPECIALISTA", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("La agenda y las visitas son solo del Médico Especialista")
    void soloElEspecialista(RolNombre rol) throws Exception {
        String token = tokenDe(rol);
        mvc.perform(get("/api/citas/mi-agenda").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/citas/{id}/visita", 1).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/visitas").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/visitas/{id}", 1).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ---- YS-169: la tarifa de consulta ----

    @Test
    @DisplayName("La visita copia la tarifa de la especialidad, y cambiarla después no altera la visita ya iniciada")
    void copiaLaTarifa() throws Exception {
        int especialidad = 5;
        Long especialidadId = idEspecialidad(especialidad);
        cambiarTarifa(especialidadId, "275.50").andExpect(status().isOk())
                .andExpect(jsonPath("$.tarifaConsulta").value(275.50));

        Especialista especialista = nuevoEspecialista(especialidad);
        Cita cita = citaEl(especialista, HOY.atTime(9, 0));
        iniciar(cita.getId(), especialista).andExpect(status().isCreated());

        cambiarTarifa(especialidadId, "300.00").andExpect(status().isOk());

        VisitaMedica visita = visitaRepository.findAll().stream()
                .filter(v -> v.getCita().getId().equals(cita.getId())).findFirst().orElseThrow();
        assertThat(visita.getCostoConsulta()).isEqualByComparingTo(new BigDecimal("275.50"));
        assertThat(visita.getEstado()).isEqualTo(EstadoVisita.ACTIVA);
    }

    @Test
    @DisplayName("Una tarifa negativa se rechaza en su campo")
    void tarifaNegativa() throws Exception {
        cambiarTarifa(idEspecialidad(5), "-1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tarifaConsulta").value("La tarifa no puede ser negativa"));
    }

    @ParameterizedTest
    @EnumSource(value = RolNombre.class, names = "ADMINISTRADOR_FUNDACION", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Las tarifas las consulta y las cambia solo la Fundación")
    void tarifasSoloDeLaFundacion(RolNombre rol) throws Exception {
        String token = tokenDe(rol);
        mvc.perform(get("/api/especialidades/tarifas").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/especialidades/{id}/tarifa", idEspecialidad(5))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tarifaConsulta\": 1}"))
                .andExpect(status().isForbidden());
    }

    // ---- apoyo ----

    /** `especialidad` es la posición en el catálogo, la misma que recibe crearSolicitud. */
    private record Especialista(Long id, Usuario usuario, int especialidad) {
    }

    private ResultActions iniciar(Long citaId, Especialista especialista) throws Exception {
        return mvc.perform(post("/api/citas/{id}/visita", citaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista)));
    }

    private ResultActions cambiarTarifa(Long especialidadId, String monto) throws Exception {
        return mvc.perform(put("/api/especialidades/{id}/tarifa", especialidadId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_FUNDACION))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tarifaConsulta\": %s}".formatted(monto)));
    }

    private EstadoSolicitud estadoDe(Cita cita) {
        return solicitudRepository.findById(cita.getSolicitud().getId()).orElseThrow().getEstado();
    }

    /**
     * Una cita del especialista en ese momento. Se asigna por la API una
     * semana adelante, a la misma hora, y después se mueve al día pedido.
     */
    private Cita citaEl(Especialista especialista, LocalDateTime momento) throws Exception {
        Long solicitud = crearSolicitud(especialista.especialidad());
        LocalDateTime asignada = SEMANA_PROXIMA.atTime(momento.toLocalTime());
        mvc.perform(post("/api/solicitudes-referencia/{id}/cita", solicitud)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_FUNDACION))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"especialistaId\": %d, \"fechaHora\": \"%s\"}".formatted(especialista.id(), asignada)))
                .andExpect(status().isCreated());

        Cita cita = citaRepository.findBySolicitudId(solicitud).orElseThrow();
        if (!momento.equals(asignada)) {
            cita.setFechaHora(momento);
            cita = citaRepository.save(cita);
        }
        return cita;
    }

    /** Un especialista nuevo de la especialidad en esa posición del catálogo, toda la semana de 8 a 12. */
    private Especialista nuevoEspecialista(int especialidad) throws Exception {
        Usuario medico = new Usuario();
        medico.setNombreCompleto("Dr. Visita " + SIGUIENTE.incrementAndGet());
        medico.setCorreo("especialista.visita" + SIGUIENTE.get() + "@asilo.gt");
        medico.setPasswordHash(passwordEncoder.encode(PASSWORD));
        medico.setRol(rolRepository.findByNombre(RolNombre.MEDICO_ESPECIALISTA).orElseThrow());
        medico.setActivo(true);
        medico.setDebeCambiarPassword(false);
        usuarioRepository.save(medico);

        List<Map<String, Object>> horarios = new ArrayList<>();
        for (DayOfWeek dia : DayOfWeek.values()) {
            horarios.add(Map.of("dia", dia.name(), "horaInicio", "08:00", "horaFin", "12:00"));
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("usuarioId", medico.getId());
        cuerpo.put("especialidadId", idEspecialidad(especialidad));
        cuerpo.put("duracionCitaMinutos", 30);
        cuerpo.put("horarios", horarios);

        String respuesta = mvc.perform(post("/api/especialistas")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_FUNDACION))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new Especialista(((Number) JsonPath.read(respuesta, "$.id")).longValue(), medico, especialidad);
    }

    private Long idEspecialidad(int posicion) {
        return especialidadRepository.findByActivaTrueOrderByNombreAsc().get(posicion).getId();
    }

    /** Una solicitud pendiente de la especialidad en esa posición, para un interno nuevo. */
    private Long crearSolicitud(int especialidad) throws Exception {
        String interno = """
                {"nombreCompleto": "Interno con visita",
                 "dpi": "%d",
                 "fechaNacimiento": "1943-02-14",
                 "fechaIngreso": "2023-05-02",
                 "familiarResponsable": {
                   "nombreCompleto": "Familiar de prueba",
                   "parentesco": "Hija",
                   "telefono": "5555 0001"}}
                """.formatted(SIGUIENTE_DPI.getAndIncrement());
        String token = tokenDe(RolNombre.MEDICO_GENERAL);
        String respuestaInterno = mvc.perform(post("/api/internos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(interno))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long internoId = ((Number) JsonPath.read(respuestaInterno, "$.id")).longValue();

        String solicitud = """
                {"internoId": %d, "especialidadId": %d, "enfermeroAcompananteId": %d,
                 "motivo": "Control de seguimiento"}
                """.formatted(internoId, idEspecialidad(especialidad), usuarioDe(RolNombre.ENFERMERIA).getId());
        String respuesta = mvc.perform(post("/api/solicitudes-referencia")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(solicitud))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private Usuario usuarioDe(RolNombre rol) {
        String correo = rol.name().toLowerCase(Locale.ROOT) + ".visitas@asilo.gt";
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
        return login(usuarioDe(rol).getCorreo());
    }

    private String tokenDe(Especialista especialista) throws Exception {
        return login(especialista.usuario().getCorreo());
    }

    private String login(String correo) throws Exception {
        String respuesta = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"%s\", \"password\": \"%s\"}".formatted(correo, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(respuesta, "$.token");
    }
}
