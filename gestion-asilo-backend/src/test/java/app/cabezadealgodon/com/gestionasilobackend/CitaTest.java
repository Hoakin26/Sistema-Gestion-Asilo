package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoNotificacion;
import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoNotificacion;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.EspecialidadRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.NotificacionFamiliarRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.RolRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import tools.jackson.databind.ObjectMapper;

/**
 * YS-93. La asignación de la cita (HU-06, RF-05, CU-002): que la solicitud
 * pase a Cita Programada, que la hora sea del horario y esté libre, que dos
 * asignaciones a la vez no tomen la misma hora, y el segundo aviso al familiar.
 *
 * <p>
 * Todas las citas van una semana adelante y el especialista atiende los siete
 * días de 8:00 a 12:00 con citas de 30 minutos: así ninguna prueba depende
 * del día de la semana en que se ejecute.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Asignación de la cita (YS-93)")
class CitaTest extends PruebaConBaseDeDatos {

    private static final String PASSWORD = "Prueba1234";
    private static final AtomicLong SIGUIENTE = new AtomicLong();
    private static final AtomicLong SIGUIENTE_DPI = new AtomicLong(6_500_000_000_000L);
    private static final LocalDate DIA = LocalDate.now().plusDays(7);

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
    private NotificacionFamiliarRepository notificacionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("La Fundación asigna la cita: la solicitud pasa a Cita Programada y la hora deja de estar libre")
    void asignaLaCita() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Long solicitud = crearSolicitud(0, null);

        asignar(solicitud, especialista.id(), DIA.atTime(9, 0))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.especialista.nombreCompleto").value(especialista.nombre()))
                .andExpect(jsonPath("$.fechaHora").value(DIA.atTime(9, 0).toString() + ":00"));

        mvc.perform(get("/api/solicitudes-referencia/{id}/cita", solicitud)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.MEDICO_GENERAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.especialista.nombreCompleto").value(especialista.nombre()));

        List<String> estados = JsonPath.read(listar(), "$.content[?(@.id == %d)].estado".formatted(solicitud));
        assertThat(estados).containsExactly("CITA_PROGRAMADA");
        assertThat(libres(especialista.id())).noneMatch(hora -> hora.startsWith("09:00"));
    }

    @Test
    @DisplayName("Una solicitud que ya tiene cita no recibe otra: 409")
    void unaSolaCitaPorSolicitud() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Long solicitud = crearSolicitud(0, null);
        asignar(solicitud, especialista.id(), DIA.atTime(8, 0)).andExpect(status().isCreated());

        asignar(solicitud, especialista.id(), DIA.atTime(8, 30)).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Dos solicitudes no pueden tomar la misma hora del mismo especialista: 409")
    void laHoraOcupadaSeRechaza() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        asignar(crearSolicitud(0, null), especialista.id(), DIA.atTime(10, 0)).andExpect(status().isCreated());

        asignar(crearSolicitud(0, null), especialista.id(), DIA.atTime(10, 0))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Una hora fuera del horario o que no es una franja se rechaza en el campo fechaHora")
    void laHoraTieneQueSerDelHorario() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Long solicitud = crearSolicitud(0, null);

        asignar(solicitud, especialista.id(), DIA.atTime(13, 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fechaHora").isNotEmpty());
        // Dentro del horario, pero a mitad de una franja de 30 minutos.
        asignar(solicitud, especialista.id(), DIA.atTime(8, 10))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fechaHora").isNotEmpty());
    }

    @Test
    @DisplayName("Una fecha que ya pasó se rechaza")
    void elPasadoSeRechaza() throws Exception {
        Especialista especialista = nuevoEspecialista(0);

        asignar(crearSolicitud(0, null), especialista.id(), LocalDate.now().minusDays(1).atTime(9, 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fechaHora").isNotEmpty());
    }

    @Test
    @DisplayName("Un especialista de otra especialidad no puede atender la solicitud")
    void laEspecialidadTieneQueCoincidir() throws Exception {
        Especialista especialista = nuevoEspecialista(1);

        asignar(crearSolicitud(0, null), especialista.id(), DIA.atTime(9, 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.especialistaId").isNotEmpty());
    }

    @Test
    @DisplayName("Una solicitud rechazada en la asignación sigue pendiente")
    void unRechazoNoCambiaElEstado() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Long solicitud = crearSolicitud(0, null);
        asignar(solicitud, especialista.id(), DIA.atTime(13, 0)).andExpect(status().isBadRequest());

        List<String> estados = JsonPath.read(listar(), "$.content[?(@.id == %d)].estado".formatted(solicitud));
        assertThat(estados).containsExactly("PENDIENTE_ASIGNACION");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = "ADMINISTRADOR_FUNDACION", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Solo la Fundación asigna citas")
    void soloLaFundacionAsigna(RolNombre rol) throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Long solicitud = crearSolicitud(0, null);

        mvc.perform(post("/api/solicitudes-referencia/{id}/cita", solicitud)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoCita(especialista.id(), DIA.atTime(9, 0))))
                .andExpect(status().isForbidden());
    }

    /**
     * El caso que el bloqueo del especialista tiene que resistir: dos personas
     * de la Fundación eligen la misma hora en el mismo instante. Una gana y la
     * otra recibe 409; nunca quedan las dos.
     */
    @Test
    @DisplayName("Dos asignaciones simultáneas a la misma hora: una pasa y la otra recibe 409")
    void lasSimultaneasNoComparteHora() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Long primera = crearSolicitud(0, null);
        Long segunda = crearSolicitud(0, null);
        String token = tokenDe(RolNombre.ADMINISTRADOR_FUNDACION);
        String cuerpo = cuerpoCita(especialista.id(), DIA.atTime(11, 0));

        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> resultados = new ArrayList<>();
            for (Long solicitud : List.of(primera, segunda)) {
                Callable<Integer> tarea = () -> {
                    largada.await();
                    return mvc.perform(post("/api/solicitudes-referencia/{id}/cita", solicitud)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                            .andReturn().getResponse().getStatus();
                };
                resultados.add(hilos.submit(tarea));
            }
            largada.countDown();

            List<Integer> codigos = new ArrayList<>();
            for (Future<Integer> resultado : resultados) {
                codigos.add(resultado.get());
            }
            assertThat(codigos).containsExactlyInAnyOrder(201, 409);
        } finally {
            hilos.shutdownNow();
        }
    }

    /** RF-04: el segundo aviso, el que informa el especialista asignado. */
    @Test
    @DisplayName("Al asignar la cita, el familiar recibe un correo con el especialista, la fecha y la hora")
    void avisaAlFamiliarDeLaCita() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        String direccion = "familia.cita" + SIGUIENTE.incrementAndGet() + "@ejemplo.com";
        Long solicitud = crearSolicitud(0, direccion);

        asignar(solicitud, especialista.id(), DIA.atTime(8, 30)).andExpect(status().isCreated());

        await().atMost(Duration.ofSeconds(10)).until(() -> notificacionRepository
                .findBySolicitudIdOrderByIdAsc(solicitud).stream()
                .anyMatch(n -> n.getTipo() == TipoNotificacion.CITA_ASIGNADA
                        && n.getEstado() == EstadoNotificacion.ENVIADA));

        String texto = textoPlano(mensajeConAsunto(direccion, "Cita programada"));
        assertThat(texto).contains(especialista.nombre(), "8:30", "Cita programada");
    }

    // ---- apoyo ----

    private record Especialista(Long id, String nombre) {
    }

    /** Un especialista nuevo de la especialidad en esa posición del catálogo, toda la semana de 8 a 12. */
    private Especialista nuevoEspecialista(int especialidad) throws Exception {
        Usuario medico = new Usuario();
        medico.setNombreCompleto("Dra. Especialista " + SIGUIENTE.incrementAndGet());
        medico.setCorreo("especialista.cita" + SIGUIENTE.get() + "@asilo.gt");
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
        return new Especialista(((Number) JsonPath.read(respuesta, "$.id")).longValue(), medico.getNombreCompleto());
    }

    private Long idEspecialidad(int posicion) {
        return especialidadRepository.findByActivaTrueOrderByNombreAsc().get(posicion).getId();
    }

    private String cuerpoCita(Long especialistaId, LocalDateTime fechaHora) {
        return "{\"especialistaId\": %d, \"fechaHora\": \"%s\"}".formatted(especialistaId, fechaHora);
    }

    private ResultActions asignar(Long solicitud, Long especialistaId, LocalDateTime fechaHora) throws Exception {
        return mvc.perform(post("/api/solicitudes-referencia/{id}/cita", solicitud)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_FUNDACION))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoCita(especialistaId, fechaHora)));
    }

    private String listar() throws Exception {
        return mvc.perform(get("/api/solicitudes-referencia").param("size", "1000")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_FUNDACION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private List<String> libres(Long especialistaId) throws Exception {
        String respuesta = mvc.perform(get("/api/especialistas/{id}/disponibilidad", especialistaId)
                .param("fecha", DIA.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_FUNDACION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(respuesta, "$");
    }

    /** Una solicitud pendiente de la especialidad en esa posición, con o sin correo del familiar. */
    private Long crearSolicitud(int especialidad, String correoFamiliar) throws Exception {
        String correo = correoFamiliar == null ? "" : ", \"correo\": \"%s\"".formatted(correoFamiliar);
        String interno = """
                {"nombreCompleto": "Interno con cita",
                 "dpi": "%d",
                 "fechaNacimiento": "1941-07-08",
                 "fechaIngreso": "2022-01-20",
                 "familiarResponsable": {
                   "nombreCompleto": "Familiar de prueba",
                   "parentesco": "Hijo",
                   "telefono": "5555 0000"%s}}
                """.formatted(SIGUIENTE_DPI.getAndIncrement(), correo);
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

    /** El mensaje enviado a una dirección cuyo asunto empieza así, entre todos los enviados. */
    private MimeMessage mensajeConAsunto(String direccion, String inicioAsunto) throws Exception {
        ArgumentCaptor<MimeMessage> enviados = ArgumentCaptor.forClass(MimeMessage.class);
        verify(correo, atLeastOnce()).send(enviados.capture());
        for (MimeMessage mensaje : enviados.getAllValues()) {
            boolean paraEl = mensaje.getRecipients(Message.RecipientType.TO)[0].toString().equals(direccion);
            if (paraEl && mensaje.getSubject().startsWith(inicioAsunto)) return mensaje;
        }
        throw new AssertionError("No se envió «" + inicioAsunto + "» a " + direccion);
    }

    private static String textoPlano(Part parte) throws Exception {
        Object contenido = parte.getContent();
        if (contenido instanceof String texto) {
            return parte.isMimeType("text/plain") ? texto : null;
        }
        if (contenido instanceof Multipart partes) {
            for (int i = 0; i < partes.getCount(); i++) {
                String texto = textoPlano(partes.getBodyPart(i));
                if (texto != null) return texto;
            }
        }
        return null;
    }

    private Usuario usuarioDe(RolNombre rol) {
        String correo = rol.name().toLowerCase(Locale.ROOT) + ".citas@asilo.gt";
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
