package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import app.cabezadealgodon.com.gestionasilobackend.models.CargoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.Cita;
import app.cabezadealgodon.com.gestionasilobackend.models.CuentaMensual;
import app.cabezadealgodon.com.gestionasilobackend.models.DetalleReceta;
import app.cabezadealgodon.com.gestionasilobackend.models.Especialidad;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoOrden;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoReceta;
import app.cabezadealgodon.com.gestionasilobackend.models.OrdenLaboratorio;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoSolicitud;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoVisita;
import app.cabezadealgodon.com.gestionasilobackend.models.Interno;
import app.cabezadealgodon.com.gestionasilobackend.models.Receta;
import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.SolicitudReferencia;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.models.VisitaMedica;
import app.cabezadealgodon.com.gestionasilobackend.repository.CargoServicioRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.CitaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.CuentaMensualRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.EspecialidadRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.ExamenLaboratorioRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.InternoRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.OrdenLaboratorioRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.PagoRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.PoliticaDescuentoRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.RecetaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.RolRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.SolicitudReferenciaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.VisitaMedicaRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * YS-101. El inicio de la visita médica (HU-07, RF-06, CU-003): que solo lo
 * haga el especialista asignado, solo sobre una cita programada, una sola vez
 * y desde el día de la cita; que la solicitud pase a En Atención y que la
 * visita copie la tarifa de la especialidad (YS-169). Desde HU-08 (YS-104),
 * también que los antecedentes del paciente solo se vean con la visita activa.
 * Desde HU-09 (YS-107), el diagnóstico obligatorio y el cierre de la visita
 * con su cargo de consulta. Desde HU-10 (YS-110), la receta, y desde HU-11
 * (YS-113), los exámenes de laboratorio, y desde HU-12 (YS-116), la bandeja
 * donde el laboratorio los recibe, desde HU-13 (YS-119) su resultado y
 * desde HU-14 (YS-122) su costo. Desde HU-15 (YS-125), la bandeja donde la
 * farmacia recibe las recetas, desde HU-16 (YS-128) su despacho y desde
 * HU-17 (YS-131) su costo. Desde HU-18 (YS-135), la cuenta del familiar que
 * reúne los cargos de los tres, desde HU-19 (YS-138), los descuentos de la
 * fundación que se le aplican, desde HU-34 (YS-178), los pagos del familiar
 * sobre esa cuenta, su anulación y la mora, y desde HU-20 (YS-141), lo que el
 * asilo le debe a la fundación por esos mismos cargos y sus pagos, y desde
 * HU-21 (YS-144), las citas que el familiar todavía no termina de pagar.
 * Desde HU-02 (YS-67), el historial médico centralizado del Médico General,
 * que se arma con todo lo anterior.
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
    private CargoServicioRepository cargoRepository;

    @Autowired
    private RecetaRepository recetaRepository;

    @Autowired
    private ExamenLaboratorioRepository examenRepository;

    @Autowired
    private OrdenLaboratorioRepository ordenRepository;

    @Autowired
    private CuentaMensualRepository cuentaRepository;

    @Autowired
    private PoliticaDescuentoRepository politicaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private InternoRepository internoRepository;

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
        mvc.perform(get("/api/visitas/{id}/historial", 1).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/visitas/{id}/diagnostico", 1).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"diagnostico\": \"x\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/visitas/{id}/finalizar", 1).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        String receta = "{\"medicamentos\": [{\"medicamento\": \"x\", \"dosis\": \"x\", \"cantidad\": 1, \"formaAplicacion\": \"x\"}]}";
        mvc.perform(post("/api/visitas/{id}/receta", 1).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(receta))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/visitas/{id}/receta", 1).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(receta))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/visitas/{id}/receta", 1).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/examenes-laboratorio").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/visitas/{id}/ordenes-laboratorio", 1).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"examenes\": [{\"examenId\": 1}]}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/visitas/{id}/ordenes-laboratorio", 1).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ---- YS-113: los exámenes de laboratorio (HU-11) ----

    @Test
    @DisplayName("El catálogo de exámenes trae los 16 de la lista base en orden alfabético, sin el costo")
    void elCatalogoDeExamenes() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        mvc.perform(get("/api/examenes-laboratorio").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(16))
                // Con tilde y todo, va primero: no al final, como con la intercalación de la base.
                .andExpect(jsonPath("$[0].nombre").value("Ácido úrico"))
                .andExpect(jsonPath("$[15].nombre").value("Vitamina B12"))
                .andExpect(jsonPath("$[?(@.nombre == 'Hematología completa')]").isNotEmpty())
                .andExpect(jsonPath("$[0].costoReferencia").doesNotExist());
    }

    @Test
    @DisplayName("Ordenar varios exámenes genera una orden pendiente por cada uno, con el costo del catálogo")
    void ordenaVariosExamenes() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(8, 0)).getId(), especialista));

        ordenar(visita, especialista, examen("Hematología completa", "En ayunas"), examen("Glucosa en ayunas", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$[0].visitaId").value(visita))
                .andExpect(jsonPath("$[0].examen.nombre").value("Hematología completa"))
                .andExpect(jsonPath("$[0].indicaciones").value("En ayunas"))
                .andExpect(jsonPath("$[1].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$[1].indicaciones").isEmpty())
                .andExpect(jsonPath("$[0].costo").doesNotExist());

        mvc.perform(get("/api/visitas/{id}/ordenes-laboratorio", visita)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        List<OrdenLaboratorio> ordenes = ordenRepository.findByVisitaIdOrderByIdAsc(visita);
        assertThat(ordenes.get(0).getCosto()).isEqualByComparingTo("75.00");
        assertThat(ordenes.get(1).getCosto()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("Un examen que no está en el catálogo se rechaza en su fila (400), y no se crea ninguna orden")
    void examenFueraDelCatalogo() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(8, 30)).getId(), especialista));

        Map<String, Object> inexistente = new LinkedHashMap<>();
        inexistente.put("examenId", 999_999);
        ordenar(visita, especialista, examen("Creatinina", null), inexistente)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['examenes[1].examenId']").value("El examen seleccionado no está en el catálogo"));

        ordenar(visita, especialista)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.examenes").value("Seleccione al menos un examen"));

        assertThat(ordenRepository.findByVisitaIdOrderByIdAsc(visita)).isEmpty();
    }

    @Test
    @DisplayName("Un examen repetido en la misma petición es 400; uno ya ordenado en la visita, 409")
    void examenRepetido() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(9, 0)).getId(), especialista));

        ordenar(visita, especialista, examen("Urocultivo", null), examen("Urocultivo", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['examenes[1].examenId']").value("Ese examen ya está en la lista"));

        ordenar(visita, especialista, examen("Urocultivo", null)).andExpect(status().isCreated());
        ordenar(visita, especialista, examen("Examen general de orina", null), examen("Urocultivo", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("ya se ordenó en esta visita")));

        assertThat(ordenRepository.findByVisitaIdOrderByIdAsc(visita)).hasSize(1);
    }

    @Test
    @DisplayName("Con la visita finalizada ya no se ordenan exámenes (409), pero se siguen pudiendo consultar")
    void conLaVisitaFinalizadaNoSeOrdena() throws Exception {
        tarifaDe(6, "410.00");
        Especialista especialista = nuevoEspecialista(6);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(9, 30)).getId(), especialista));
        ordenar(visita, especialista, examen("Ácido úrico", null)).andExpect(status().isCreated());
        diagnosticar(visita, especialista, "Gota", null).andExpect(status().isOk());
        finalizar(visita, especialista).andExpect(status().isOk());

        ordenar(visita, especialista, examen("Creatinina", null)).andExpect(status().isConflict());
        mvc.perform(get("/api/visitas/{id}/ordenes-laboratorio", visita)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Otro especialista no ordena ni consulta los exámenes de una visita ajena: 403")
    void otroEspecialistaNoOrdena() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Especialista otro = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(10, 0)).getId(), especialista));

        ordenar(visita, otro, examen("Creatinina", null)).andExpect(status().isForbidden());
        mvc.perform(get("/api/visitas/{id}/ordenes-laboratorio", visita)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(otro)))
                .andExpect(status().isForbidden());
        assertThat(ordenRepository.findByVisitaIdOrderByIdAsc(visita)).isEmpty();
    }

    /** HU-08: "Mostrar exámenes de laboratorio previos", su último criterio. */
    @Test
    @DisplayName("Los antecedentes traen los exámenes de visitas anteriores, sin los de la visita en curso")
    void losAntecedentesTraenLosExamenes() throws Exception {
        Especialista anterior = nuevoEspecialista(0);
        Especialista actual = nuevoEspecialista(0);
        Cita antigua = citaEl(anterior, HOY.minusDays(4).atTime(8, 0), null);
        Long interno = antigua.getSolicitud().getInterno().getId();
        Cita deHoy = citaEl(actual, HOY.atTime(10, 30), interno);

        long visitaAntigua = idDe(iniciar(antigua.getId(), anterior));
        ordenar(visitaAntigua, anterior, examen("Perfil lipídico", "En ayunas de 12 horas")).andExpect(status().isCreated());
        cerrar(visitaAntigua, "Dislipidemia", HOY.minusDays(4).atTime(8, 5));

        long enCurso = idDe(iniciar(deHoy.getId(), actual));
        ordenar(enCurso, actual, examen("Hematología completa", null)).andExpect(status().isCreated());

        mvc.perform(get("/api/visitas/{id}/historial", enCurso).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(actual)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examenes.length()").value(1))
                .andExpect(jsonPath("$.examenes[0].examen.nombre").value("Perfil lipídico"))
                .andExpect(jsonPath("$.examenes[0].indicaciones").value("En ayunas de 12 horas"))
                .andExpect(jsonPath("$.examenes[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.examenes[0].especialista.nombreCompleto")
                        .value(anterior.usuario().getNombreCompleto()));
    }

    // ---- YS-116: la bandeja del laboratorio (HU-12) ----

    /** Lo que pide HU-12 de cada orden: el examen y el médico que lo solicitó, además del paciente. */
    @Test
    @DisplayName("La bandeja trae cada orden con el paciente, el examen, las indicaciones y el médico que la pidió, sin diagnóstico ni costo")
    void laBandejaTraeLoQuePideHu12() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Cita cita = citaEl(especialista, HOY.atTime(8, 0));
        long visita = idDe(iniciar(cita.getId(), especialista));
        diagnosticar(visita, especialista, "Anemia en estudio", null).andExpect(status().isOk());
        ordenar(visita, especialista, examen("Hematología completa", "En ayunas")).andExpect(status().isCreated());

        mvc.perform(get("/api/ordenes-laboratorio").param("paciente", dpiDe(cita))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.LABORATORIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].paciente.nombreCompleto").value("Interno con visita"))
                .andExpect(jsonPath("$.content[0].medicoSolicitante.nombreCompleto")
                        .value(especialista.usuario().getNombreCompleto()))
                .andExpect(jsonPath("$.content[0].examen.nombre").value("Hematología completa"))
                .andExpect(jsonPath("$.content[0].indicaciones").value("En ayunas"))
                .andExpect(jsonPath("$.content[0].estado").value("PENDIENTE"))
                // Mínimo privilegio (sección 4.9 del diseño): ni el diagnóstico ni el costo ni el DPI.
                .andExpect(jsonPath("$.content[0].diagnostico").doesNotExist())
                .andExpect(jsonPath("$.content[0].costo").doesNotExist())
                .andExpect(jsonPath("$.content[0].paciente.dpi").doesNotExist());
    }

    @Test
    @DisplayName("El filtro por estado devuelve solo las de ese estado, y repetido junta varios")
    void filtraLaBandejaPorEstado() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Cita cita = citaEl(especialista, HOY.atTime(8, 30));
        long visita = idDe(iniciar(cita.getId(), especialista));
        ordenar(visita, especialista, examen("Creatinina", null), examen("Glucosa en ayunas", null))
                .andExpect(status().isCreated());
        List<OrdenLaboratorio> ordenes = ordenRepository.findByVisitaIdOrderByIdAsc(visita);
        Long tomada = ordenes.get(0).getId();
        Long pendiente = ordenes.get(1).getId();
        iniciarOrden(tomada).andExpect(status().isOk());

        String dpi = dpiDe(cita);
        assertThat(idsEnBandeja(dpi, "PENDIENTE")).containsExactly(pendiente.intValue());
        assertThat(idsEnBandeja(dpi, "EN_PROCESO")).containsExactly(tomada.intValue());
        assertThat(idsEnBandeja(dpi, "RESULTADOS_CARGADOS")).isEmpty();
        assertThat(idsEnBandeja(dpi, "PENDIENTE", "EN_PROCESO"))
                .containsExactly(tomada.intValue(), pendiente.intValue());
        assertThat(idsEnBandeja(dpi)).containsExactly(tomada.intValue(), pendiente.intValue());
    }

    /** Como se busca al llegar al laboratorio: por el DPI del documento, o por el nombre como salga escrito. */
    @Test
    @DisplayName("La búsqueda encuentra al paciente por DPI y por nombre, sin mayúsculas ni tildes")
    void buscaLaBandejaPorPaciente() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Cita cita = citaEl(especialista, HOY.atTime(9, 0));
        long visita = idDe(iniciar(cita.getId(), especialista));
        ordenar(visita, especialista, examen("Urocultivo", null)).andExpect(status().isCreated());
        int orden = ordenRepository.findByVisitaIdOrderByIdAsc(visita).get(0).getId().intValue();

        assertThat(idsEnBandeja(dpiDe(cita))).containsExactly(orden);
        assertThat(idsEnBandeja("INTERNO CON VÍSITA")).contains(orden);
        assertThat(idsEnBandeja("zzz-no-existe")).isEmpty();
    }

    @Test
    @DisplayName("Iniciar una orden pendiente la pasa a En Proceso y anota quién la tomó; una segunda vez es 409")
    void iniciaUnaOrden() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(9, 30)).getId(), especialista));
        ordenar(visita, especialista, examen("Perfil lipídico", null)).andExpect(status().isCreated());
        Long orden = ordenRepository.findByVisitaIdOrderByIdAsc(visita).get(0).getId();

        iniciarOrden(orden)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"));
        assertThat(ordenRepository.findById(orden).orElseThrow().getLaboratorista().getId())
                .isEqualTo(usuarioDe(RolNombre.LABORATORIO).getId());

        iniciarOrden(orden)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("está en «En Proceso»")));
        iniciarOrden(999_999L).andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = "LABORATORIO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Los otros seis roles no consultan la bandeja ni toman órdenes: 403")
    void soloElLaboratorioUsaLaBandeja(RolNombre rol) throws Exception {
        mvc.perform(get("/api/ordenes-laboratorio").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol)))
                .andExpect(status().isForbidden());
        // El rol se revisa antes de buscar la orden: no hace falta que exista.
        mvc.perform(patch("/api/ordenes-laboratorio/{id}/iniciar", 999_999)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("La bandeja sin token es 401, y ordenarla por un campo que no existe, 400")
    void bandejaSinTokenYOrdenInvalido() throws Exception {
        mvc.perform(get("/api/ordenes-laboratorio")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/ordenes-laboratorio").param("sort", "campoInventado")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.LABORATORIO)))
                .andExpect(status().isBadRequest());
    }

    // ---- YS-119: el resultado de laboratorio (HU-13) ----

    /** Un PDF mínimo: lo que lo identifica son sus primeros bytes, "%PDF-". */
    private static final byte[] PDF = "%PDF-1.4\n% resultado de prueba\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13 };

    @Test
    @DisplayName("Cargar el resultado con un PDF pasa la orden a Resultados Cargados, y el especialista lo ve en su visita y descarga el archivo")
    void cargaElResultado() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(8, 0)).getId(), especialista));
        ordenar(visita, especialista, examen("Hematología completa", null)).andExpect(status().isCreated());
        Long orden = ordenRepository.findByVisitaIdOrderByIdAsc(visita).get(0).getId();
        iniciarOrden(orden).andExpect(status().isOk());

        cargarResultado(orden, "Hemoglobina 13.2 g/dL. Sin alteraciones.", archivo("informe.pdf", "application/pdf", PDF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESULTADOS_CARGADOS"))
                .andExpect(jsonPath("$.resultado").value("Hemoglobina 13.2 g/dL. Sin alteraciones."))
                .andExpect(jsonPath("$.fechaResultado").isNotEmpty())
                .andExpect(jsonPath("$.archivo.nombre").value("informe.pdf"))
                .andExpect(jsonPath("$.archivo.tipo").value("application/pdf"))
                .andExpect(jsonPath("$.archivo.tamano").value(PDF.length));

        // "Dejar el resultado visible para el médico solicitante."
        mvc.perform(get("/api/visitas/{id}/ordenes-laboratorio", visita)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resultado").value("Hemoglobina 13.2 g/dL. Sin alteraciones."))
                .andExpect(jsonPath("$[0].archivo.nombre").value("informe.pdf"));

        byte[] descargado = descargar(orden, tokenDe(especialista))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("informe.pdf")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(descargado).isEqualTo(PDF);
    }

    @Test
    @DisplayName("Una orden pendiente acepta el resultado sin archivo, y quien lo carga queda como quien la atendió")
    void resultadoSinArchivoDesdePendiente() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(8, 30)).getId(), especialista));
        ordenar(visita, especialista, examen("Glucosa en ayunas", null)).andExpect(status().isCreated());
        Long orden = ordenRepository.findByVisitaIdOrderByIdAsc(visita).get(0).getId();

        cargarResultado(orden, "Glucosa 92 mg/dL", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESULTADOS_CARGADOS"))
                .andExpect(jsonPath("$.archivo").doesNotExist());
        assertThat(ordenRepository.findById(orden).orElseThrow().getLaboratorista().getId())
                .isEqualTo(usuarioDe(RolNombre.LABORATORIO).getId());

        descargar(orden, tokenDe(RolNombre.LABORATORIO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Este resultado no tiene archivo adjunto"));
    }

    @Test
    @DisplayName("Una orden con resultado no acepta otro (409); el resultado vacío se rechaza en su campo, y una orden inexistente es 404")
    void resultadoRepetidoOVacio() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(9, 0)).getId(), especialista));
        ordenar(visita, especialista, examen("Creatinina", null)).andExpect(status().isCreated());
        Long orden = ordenRepository.findByVisitaIdOrderByIdAsc(visita).get(0).getId();

        cargarResultado(orden, "   ", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.resultado").value("Escriba el resultado del examen"));
        cargarResultado(orden, "Creatinina 0.9 mg/dL", null).andExpect(status().isOk());
        cargarResultado(orden, "Otro resultado", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("está en «Resultados Cargados»")));
        cargarResultado(999_999L, "Sin orden", null).andExpect(status().isNotFound());

        assertThat(ordenRepository.findById(orden).orElseThrow().getResultado()).isEqualTo("Creatinina 0.9 mg/dL");
    }

    /** Lo que identifica el tipo es el contenido: un ejecutable renombrado a .pdf no pasa. */
    @Test
    @DisplayName("Un archivo de otro tipo, aunque se llame .pdf, o de más de 10 MB, se rechaza en su campo y la orden sigue sin resultado")
    void archivoNoPermitido() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(9, 30)).getId(), especialista));
        ordenar(visita, especialista, examen("Urocultivo", null)).andExpect(status().isCreated());
        Long orden = ordenRepository.findByVisitaIdOrderByIdAsc(visita).get(0).getId();

        byte[] ejecutable = "MZ\u0090 no soy un PDF".getBytes(StandardCharsets.ISO_8859_1);
        cargarResultado(orden, "Negativo", archivo("resultado.pdf", "application/pdf", ejecutable))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.archivo").value("Solo se aceptan archivos PDF, JPG o PNG"));

        byte[] grande = new byte[10 * 1024 * 1024 + 1];
        System.arraycopy(PDF, 0, grande, 0, PDF.length);
        cargarResultado(orden, "Negativo", archivo("escaneo.pdf", "application/pdf", grande))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.archivo").value(containsString("10 MB")));

        OrdenLaboratorio sinResultado = ordenRepository.findById(orden).orElseThrow();
        assertThat(sinResultado.getEstado()).isEqualTo(EstadoOrden.PENDIENTE);
        assertThat(sinResultado.getResultado()).isNull();
    }

    /** Decidido el 19/10: los médicos ven el resultado; el especialista, el de sus pacientes. */
    @Test
    @DisplayName("El archivo lo descargan el laboratorio, el Médico General y el especialista que lo pidió; otro especialista, solo con una visita activa del mismo paciente")
    void quienDescargaElArchivo() throws Exception {
        Especialista propio = nuevoEspecialista(0);
        Especialista ajeno = nuevoEspecialista(1);
        Especialista conVisita = nuevoEspecialista(1);
        Cita cita = citaEl(propio, HOY.atTime(10, 0), null);
        Long interno = cita.getSolicitud().getInterno().getId();
        long visita = idDe(iniciar(cita.getId(), propio));
        ordenar(visita, propio, examen("Perfil lipídico", null)).andExpect(status().isCreated());
        Long orden = ordenRepository.findByVisitaIdOrderByIdAsc(visita).get(0).getId();
        cargarResultado(orden, "Colesterol total 180 mg/dL", archivo("foto.png", "image/png", PNG))
                .andExpect(status().isOk());

        descargar(orden, tokenDe(RolNombre.LABORATORIO)).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"));
        descargar(orden, tokenDe(RolNombre.MEDICO_GENERAL)).andExpect(status().isOk());
        descargar(orden, tokenDe(propio)).andExpect(status().isOk());
        descargar(orden, tokenDe(ajeno)).andExpect(status().isForbidden());

        // Con una visita activa con el mismo paciente, el resultado es parte de sus antecedentes.
        idDe(iniciar(citaEl(conVisita, HOY.atTime(10, 30), interno).getId(), conVisita));
        descargar(orden, tokenDe(conVisita)).andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = { "ADMINISTRADOR_ASILO", "ADMINISTRADOR_FUNDACION", "ENFERMERIA", "FARMACIA" })
    @DisplayName("El Administrador del Asilo y los demás roles que no son médicos ni laboratorio no descargan el archivo: 403")
    void archivoSoloParaLaboratorioYMedicos(RolNombre rol) throws Exception {
        // El rol se revisa antes de buscar la orden: no hace falta que exista.
        descargar(999_999L, tokenDe(rol)).andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = "LABORATORIO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Solo el laboratorio carga resultados: los otros seis roles reciben 403")
    void soloElLaboratorioCargaResultados(RolNombre rol) throws Exception {
        mvc.perform(multipart("/api/ordenes-laboratorio/{id}/resultado", 999_999).param("resultado", "Negativo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol)))
                .andExpect(status().isForbidden());
    }

    /** HU-08 y HU-02 ya traían los exámenes anteriores; desde HU-13, con su resultado. */
    @Test
    @DisplayName("Los antecedentes de la visita y el historial centralizado muestran el resultado de los exámenes anteriores")
    void losAntecedentesTraenElResultado() throws Exception {
        Especialista anterior = nuevoEspecialista(0);
        Especialista actual = nuevoEspecialista(0);
        Cita antigua = citaEl(anterior, HOY.minusDays(3).atTime(8, 0), null);
        Long interno = antigua.getSolicitud().getInterno().getId();
        long visitaAntigua = idDe(iniciar(antigua.getId(), anterior));
        ordenar(visitaAntigua, anterior, examen("Ácido úrico", null)).andExpect(status().isCreated());
        cerrar(visitaAntigua, "Gota en estudio", HOY.minusDays(3).atTime(8, 5));
        // Lo normal: el laboratorio entrega después de que la consulta terminó.
        Long orden = ordenRepository.findByVisitaIdOrderByIdAsc(visitaAntigua).get(0).getId();
        cargarResultado(orden, "Ácido úrico 7.9 mg/dL, elevado", archivo("acido-urico.pdf", "application/pdf", PDF))
                .andExpect(status().isOk());

        long enCurso = idDe(iniciar(citaEl(actual, HOY.atTime(11, 0), interno).getId(), actual));
        mvc.perform(get("/api/visitas/{id}/historial", enCurso)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(actual)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examenes[0].resultado").value("Ácido úrico 7.9 mg/dL, elevado"))
                .andExpect(jsonPath("$.examenes[0].archivo.nombre").value("acido-urico.pdf"));

        mvc.perform(get("/api/internos/{id}/historial", interno)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.MEDICO_GENERAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examenes[0].resultado").value("Ácido úrico 7.9 mg/dL, elevado"))
                .andExpect(jsonPath("$.examenes[0].estado").value("RESULTADOS_CARGADOS"));
    }

    // ---- YS-122: el costo del laboratorio (HU-14) ----

    @Test
    @DisplayName("El costo que registra el laboratorio queda en la orden y como cargo de laboratorio de la visita, una sola vez")
    void elCostoQuedaComoCargo() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Cita cita = citaEl(especialista, HOY.atTime(11, 0));
        long visita = idDe(iniciar(cita.getId(), especialista));
        ordenar(visita, especialista, examen("Hematología completa", null)).andExpect(status().isCreated());
        Long orden = ordenRepository.findByVisitaIdOrderByIdAsc(visita).get(0).getId();

        // Antes del resultado, el sugerido: el costo de referencia del catálogo (RF-08 v1.1).
        costoDe(orden)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costo").value(75.00))
                .andExpect(jsonPath("$.registrado").value(false));

        cargarResultado(orden, "Hemoglobina 12.9 g/dL", "80,50", null).andExpect(status().isOk());

        List<CargoServicio> cargos = cargosDeLaboratorio(visita);
        assertThat(cargos).hasSize(1);
        assertThat(cargos.get(0).getMontoOriginal()).isEqualByComparingTo("80.50");
        assertThat(cargos.get(0).getDescuento()).isEqualByComparingTo("0");
        assertThat(cargos.get(0).getMontoNeto()).isEqualByComparingTo("80.50");
        assertThat(cargos.get(0).getInterno().getId()).isEqualTo(cita.getSolicitud().getInterno().getId());
        assertThat(ordenRepository.findById(orden).orElseThrow().getCosto()).isEqualByComparingTo("80.50");

        costoDe(orden)
                .andExpect(jsonPath("$.costo").value(80.50))
                .andExpect(jsonPath("$.registrado").value(true));

        // "El costo se registra una sola vez": otro resultado es 409 y no hay segundo cargo.
        cargarResultado(orden, "Otro resultado", "90", null).andExpect(status().isConflict());
        assertThat(cargosDeLaboratorio(visita)).hasSize(1);

        // El especialista sigue sin ver costos (decidido el 19/10).
        mvc.perform(get("/api/visitas/{id}/ordenes-laboratorio", visita)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista)))
                .andExpect(jsonPath("$[0].resultado").value("Hemoglobina 12.9 g/dL"))
                .andExpect(jsonPath("$[0].costo").doesNotExist());
    }

    @Test
    @DisplayName("Un costo vacío, en cero, negativo o mal escrito se rechaza en su campo, y la orden queda sin resultado ni cargo")
    void costoInvalido() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(11, 30)).getId(), especialista));
        ordenar(visita, especialista, examen("Glucosa en ayunas", null)).andExpect(status().isCreated());
        Long orden = ordenRepository.findByVisitaIdOrderByIdAsc(visita).get(0).getId();

        Map<String, String> esperados = new LinkedHashMap<>();
        esperados.put("", "Indique el costo del examen");
        esperados.put("0", "El costo tiene que ser mayor que cero");
        esperados.put("0.00", "El costo tiene que ser mayor que cero");
        esperados.put("-30", "El costo tiene que ser mayor que cero");
        esperados.put("treinta", "Escriba un monto como 75 o 75.50");
        esperados.put("30.555", "Escriba un monto como 75 o 75.50");
        esperados.put("1e3", "Escriba un monto como 75 o 75.50");
        for (Map.Entry<String, String> caso : esperados.entrySet()) {
            cargarResultado(orden, "Glucosa 90 mg/dL", caso.getKey(), null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.costo").value(caso.getValue()));
        }

        assertThat(ordenRepository.findById(orden).orElseThrow().getEstado()).isEqualTo(EstadoOrden.PENDIENTE);
        assertThat(cargosDeLaboratorio(visita)).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = "LABORATORIO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("El costo de una orden solo lo consulta el laboratorio: los otros seis roles reciben 403")
    void soloElLaboratorioVeElCosto(RolNombre rol) throws Exception {
        mvc.perform(get("/api/ordenes-laboratorio/{id}/costo", 999_999)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol)))
                .andExpect(status().isForbidden());
    }

    // ---- YS-110: la receta (HU-10) ----

    @Test
    @DisplayName("El especialista registra la receta: queda pendiente de despacho, asociada a la visita y con sus medicamentos en orden")
    void registraLaReceta() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(8, 0)).getId(), especialista));

        recetar(visita, especialista, "Tomar con alimentos",
                medicamento("Losartán", "50 mg", 30, "Cada 24 horas, vía oral"),
                medicamento("Amlodipino", "5 mg", 30, "Cada 24 horas por la mañana"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_DESPACHO"))
                .andExpect(jsonPath("$.visitaId").value(visita))
                .andExpect(jsonPath("$.indicacionesGenerales").value("Tomar con alimentos"))
                .andExpect(jsonPath("$.fechaEmision").isNotEmpty())
                .andExpect(jsonPath("$.medicamentos.length()").value(2))
                .andExpect(jsonPath("$.medicamentos[0].medicamento").value("Losartán"))
                .andExpect(jsonPath("$.medicamentos[0].formaAplicacion").value("Cada 24 horas, vía oral"))
                .andExpect(jsonPath("$.medicamentos[1].cantidad").value(30));

        mvc.perform(get("/api/visitas/{id}/receta", visita).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicamentos[1].medicamento").value("Amlodipino"));
    }

    @Test
    @DisplayName("Si falta un campo de un medicamento: 400 señalando la fila, y la receta no se guarda")
    void camposObligatoriosPorFila() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(8, 30)).getId(), especialista));

        recetar(visita, especialista, null,
                medicamento("Losartán", "50 mg", 30, "Cada 24 horas"),
                medicamento("Paracetamol", "  ", 0, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['medicamentos[1].dosis']").value("La dosis es obligatoria"))
                .andExpect(jsonPath("$.errores['medicamentos[1].cantidad']").value("La cantidad tiene que ser al menos 1"))
                .andExpect(jsonPath("$.errores['medicamentos[1].formaAplicacion']")
                        .value("El tiempo o la forma de aplicación es obligatorio"))
                .andExpect(jsonPath("$.errores['medicamentos[0].dosis']").doesNotExist());

        recetar(visita, especialista, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.medicamentos").value("Agregue al menos un medicamento"));

        assertThat(recetaRepository.existsByVisitaId(visita)).isFalse();
    }

    @Test
    @DisplayName("Una visita tiene una sola receta (409 a la segunda), y se corrige entera mientras siga pendiente")
    void unaRecetaPorVisitaQueSeCorrige() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(9, 0)).getId(), especialista));
        recetar(visita, especialista, null,
                medicamento("Losartán", "50 mg", 30, "Cada 24 horas"),
                medicamento("Amlodipino", "5 mg", 30, "Cada 24 horas")).andExpect(status().isCreated());

        recetar(visita, especialista, null, medicamento("Otro", "1", 1, "x"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("ya tiene receta")));

        corregir(visita, especialista, "Control en un mes", medicamento("Losartán", "100 mg", 30, "Cada 24 horas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicamentos.length()").value(1))
                .andExpect(jsonPath("$.medicamentos[0].dosis").value("100 mg"))
                .andExpect(jsonPath("$.indicacionesGenerales").value("Control en un mes"));
        assertThat(recetaRepository.findByVisitaId(visita).orElseThrow().getMedicamentos()).hasSize(1);
    }

    @Test
    @DisplayName("Una receta que la farmacia ya empezó a despachar no se corrige: 409")
    void laDespachadaNoSeCorrige() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(9, 30)).getId(), especialista));
        recetar(visita, especialista, null,
                medicamento("Losartán", "50 mg", 30, "Cada 24 horas"),
                medicamento("Amlodipino", "5 mg", 30, "Cada 24 horas")).andExpect(status().isCreated());
        Receta receta = recetaRepository.findByVisitaId(visita).orElseThrow();
        despachar(receta.getId(), idsDe(receta)[0])
                .andExpect(jsonPath("$.estado").value("PARCIALMENTE_ENTREGADA"));

        corregir(visita, especialista, null, medicamento("Otro", "1", 1, "x"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("ya empezó a despachar")));
    }

    @Test
    @DisplayName("Con la visita finalizada no se registra ni se corrige la receta (409), pero se sigue pudiendo consultar")
    void conLaVisitaFinalizada() throws Exception {
        tarifaDe(6, "410.00");
        Especialista especialista = nuevoEspecialista(6);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(10, 0)).getId(), especialista));
        recetar(visita, especialista, null, medicamento("Diclofenaco", "50 mg", 10, "Cada 12 horas por 5 días"))
                .andExpect(status().isCreated());
        diagnosticar(visita, especialista, "Lumbalgia", null).andExpect(status().isOk());
        finalizar(visita, especialista).andExpect(status().isOk());

        recetar(visita, especialista, null, medicamento("Otro", "1", 1, "x")).andExpect(status().isConflict());
        corregir(visita, especialista, null, medicamento("Otro", "1", 1, "x")).andExpect(status().isConflict());
        mvc.perform(get("/api/visitas/{id}/receta", visita).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicamentos[0].medicamento").value("Diclofenaco"));
    }

    @Test
    @DisplayName("Otro especialista no registra ni consulta la receta de una visita ajena: 403")
    void otroEspecialistaNoReceta() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Especialista otro = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(10, 30)).getId(), especialista));

        recetar(visita, otro, null, medicamento("Losartán", "50 mg", 30, "Cada 24 horas"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/visitas/{id}/receta", visita).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(otro)))
                .andExpect(status().isForbidden());
        assertThat(recetaRepository.existsByVisitaId(visita)).isFalse();
    }

    /** HU-08: "Mostrar medicamentos recetados anteriormente", que esperaba a esta historia. */
    @Test
    @DisplayName("Los antecedentes traen las recetas de visitas anteriores, sin la de la visita en curso")
    void losAntecedentesTraenLasRecetas() throws Exception {
        Especialista anterior = nuevoEspecialista(0);
        Especialista actual = nuevoEspecialista(0);
        Cita antigua = citaEl(anterior, HOY.minusDays(3).atTime(8, 0), null);
        Long interno = antigua.getSolicitud().getInterno().getId();
        Cita deHoy = citaEl(actual, HOY.atTime(11, 0), interno);

        long visitaAntigua = idDe(iniciar(antigua.getId(), anterior));
        recetar(visitaAntigua, anterior, "Tomar con alimentos",
                medicamento("Losartán", "50 mg", 30, "Cada 24 horas")).andExpect(status().isCreated());
        cerrar(visitaAntigua, "Hipertensión arterial", HOY.minusDays(3).atTime(8, 5));

        long enCurso = idDe(iniciar(deHoy.getId(), actual));
        recetar(enCurso, actual, null, medicamento("Metformina", "850 mg", 60, "Cada 12 horas"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/visitas/{id}/historial", enCurso).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(actual)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recetas.length()").value(1))
                .andExpect(jsonPath("$.recetas[0].medicamentos[0].medicamento").value("Losartán"))
                .andExpect(jsonPath("$.recetas[0].indicacionesGenerales").value("Tomar con alimentos"))
                .andExpect(jsonPath("$.recetas[0].estado").value("PENDIENTE_DESPACHO"))
                .andExpect(jsonPath("$.recetas[0].especialista.nombreCompleto")
                        .value(anterior.usuario().getNombreCompleto()));
    }

    // ---- YS-125: la bandeja de la farmacia (HU-15) ----

    /** Lo que pide HU-15 de cada receta: el paciente, y cada medicamento con su dosis y cantidad. */
    @Test
    @DisplayName("La bandeja trae cada receta con el paciente, el médico, la fecha y los medicamentos con dosis y cantidad, sin diagnóstico")
    void laBandejaDeFarmaciaTraeLoQuePideHu15() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Cita cita = citaEl(especialista, HOY.atTime(8, 0));
        long visita = idDe(iniciar(cita.getId(), especialista));
        diagnosticar(visita, especialista, "Hipertensión arterial", null).andExpect(status().isOk());
        recetar(visita, especialista, "Tomar con alimentos",
                medicamento("Losartán", "50 mg", 30, "Cada 24 horas, vía oral"),
                medicamento("Amlodipino", "5 mg", 15, "Cada 24 horas por la mañana"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/recetas").param("paciente", dpiDe(cita))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.FARMACIA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].visitaId").value(visita))
                .andExpect(jsonPath("$.content[0].paciente.nombreCompleto").value("Interno con visita"))
                .andExpect(jsonPath("$.content[0].medico.nombreCompleto")
                        .value(especialista.usuario().getNombreCompleto()))
                .andExpect(jsonPath("$.content[0].fechaEmision").isNotEmpty())
                .andExpect(jsonPath("$.content[0].estado").value("PENDIENTE_DESPACHO"))
                .andExpect(jsonPath("$.content[0].indicacionesGenerales").value("Tomar con alimentos"))
                .andExpect(jsonPath("$.content[0].medicamentos.length()").value(2))
                .andExpect(jsonPath("$.content[0].medicamentos[0].medicamento").value("Losartán"))
                .andExpect(jsonPath("$.content[0].medicamentos[0].dosis").value("50 mg"))
                .andExpect(jsonPath("$.content[0].medicamentos[0].cantidad").value(30))
                .andExpect(jsonPath("$.content[0].medicamentos[1].medicamento").value("Amlodipino"))
                .andExpect(jsonPath("$.content[0].medicamentos[1].cantidad").value(15))
                // Mínimo privilegio (sección 4.9 del diseño): ni el diagnóstico ni el DPI.
                .andExpect(jsonPath("$.content[0].diagnostico").doesNotExist())
                .andExpect(jsonPath("$.content[0].paciente.dpi").doesNotExist());
    }

    @Test
    @DisplayName("El filtro por estado devuelve solo las de ese estado, y repetido junta varios")
    void filtraLasRecetasPorEstado() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Cita primera = citaEl(especialista, HOY.atTime(8, 30));
        Cita segunda = citaEl(especialista, HOY.atTime(9, 0), primera.getSolicitud().getInterno().getId());
        long visitaPrimera = idDe(iniciar(primera.getId(), especialista));
        recetar(visitaPrimera, especialista, null, medicamento("Losartán", "50 mg", 30, "Cada 24 horas"))
                .andExpect(status().isCreated());
        cerrar(visitaPrimera, "Hipertensión arterial", HOY.atTime(8, 30));
        long visitaSegunda = idDe(iniciar(segunda.getId(), especialista));
        recetar(visitaSegunda, especialista, null, medicamento("Metformina", "850 mg", 60, "Cada 12 horas"))
                .andExpect(status().isCreated());

        Receta entregada = recetaRepository.findByVisitaId(visitaPrimera).orElseThrow();
        despachar(entregada.getId(), idsDe(entregada)).andExpect(jsonPath("$.estado").value("ENTREGADA"));
        int deLaPrimera = entregada.getId().intValue();
        int deLaSegunda = recetaRepository.findByVisitaId(visitaSegunda).orElseThrow().getId().intValue();

        String dpi = dpiDe(primera);
        assertThat(idsDeRecetas(dpi, "PENDIENTE_DESPACHO")).containsExactly(deLaSegunda);
        assertThat(idsDeRecetas(dpi, "ENTREGADA")).containsExactly(deLaPrimera);
        assertThat(idsDeRecetas(dpi, "PARCIALMENTE_ENTREGADA")).isEmpty();
        // De la más antigua a la más reciente.
        assertThat(idsDeRecetas(dpi, "PENDIENTE_DESPACHO", "ENTREGADA")).containsExactly(deLaPrimera, deLaSegunda);
        assertThat(idsDeRecetas(dpi)).containsExactly(deLaPrimera, deLaSegunda);
    }

    /** Como se busca en la farmacia: por el DPI del documento, o por el nombre como salga escrito. */
    @Test
    @DisplayName("La búsqueda encuentra al paciente por DPI y por nombre, sin mayúsculas ni tildes, y un comodín cuenta como texto")
    void buscaLasRecetasPorPaciente() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Cita cita = citaEl(especialista, HOY.atTime(9, 30));
        long visita = idDe(iniciar(cita.getId(), especialista));
        recetar(visita, especialista, null, medicamento("Omeprazol", "20 mg", 14, "En ayunas"))
                .andExpect(status().isCreated());
        int receta = recetaRepository.findByVisitaId(visita).orElseThrow().getId().intValue();

        assertThat(idsDeRecetas(dpiDe(cita))).containsExactly(receta);
        assertThat(idsDeRecetas("INTERNO CON VÍSITA")).contains(receta);
        assertThat(idsDeRecetas("zzz-no-existe")).isEmpty();
        assertThat(idsDeRecetas("%")).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = "FARMACIA", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Los otros seis roles no consultan la bandeja de la farmacia: 403")
    void soloLaFarmaciaVeLasRecetas(RolNombre rol) throws Exception {
        mvc.perform(get("/api/recetas").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Las recetas sin token son 401; ordenarlas por un campo que no existe o filtrar por un estado que no está en DE-03, 400")
    void recetasSinTokenYParametrosInvalidos() throws Exception {
        mvc.perform(get("/api/recetas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/recetas").param("sort", "campoInventado")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.FARMACIA)))
                .andExpect(status().isBadRequest());
        // HU-16 dice "Despachada"; DE-03 la llama Entregada (hallazgo 1 del diseño).
        mvc.perform(get("/api/recetas").param("estado", "DESPACHADA")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.FARMACIA)))
                .andExpect(status().isBadRequest());
    }

    // ---- YS-128: el despacho de la farmacia (HU-16) ----

    @Test
    @DisplayName("Despachar todos los medicamentos deja la receta Entregada, con su fecha y quién la despachó; despacharla otra vez es 409")
    void despacharTodoLaDejaEntregada() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(10, 0)).getId(), especialista));
        recetar(visita, especialista, null,
                medicamento("Losartán", "50 mg", 30, "Cada 24 horas"),
                medicamento("Amlodipino", "5 mg", 30, "Cada 24 horas")).andExpect(status().isCreated());
        Receta receta = recetaRepository.findByVisitaId(visita).orElseThrow();
        Long[] todos = idsDe(receta);

        despachar(receta.getId(), todos)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADA"))
                .andExpect(jsonPath("$.fechaDespacho").isNotEmpty())
                .andExpect(jsonPath("$.medicamentos[0].entregado").value(true))
                .andExpect(jsonPath("$.medicamentos[0].fechaEntrega").isNotEmpty())
                .andExpect(jsonPath("$.medicamentos[1].entregado").value(true));
        Receta guardada = recetaRepository.findByVisitaId(visita).orElseThrow();
        assertThat(guardada.getFechaDespacho()).isNotNull();
        assertThat(guardada.getFarmaceutico().getId()).isEqualTo(usuarioDe(RolNombre.FARMACIA).getId());

        // HU-16: "Validar que no se despache dos veces la misma receta".
        despachar(receta.getId(), todos)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("ya se entregó completa")));
        despachar(receta.getId()).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Despachar una parte la deja Parcialmente Entregada; el resto se entrega después, sin volver a entregar lo ya entregado")
    void despacharUnaParteYDespuesElResto() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(10, 30)).getId(), especialista));
        recetar(visita, especialista, null,
                medicamento("Losartán", "50 mg", 30, "Cada 24 horas"),
                medicamento("Amlodipino", "5 mg", 30, "Cada 24 horas"),
                medicamento("Metformina", "850 mg", 60, "Cada 12 horas")).andExpect(status().isCreated());
        Receta receta = recetaRepository.findByVisitaId(visita).orElseThrow();
        Long[] ids = idsDe(receta);

        despachar(receta.getId(), ids[0])
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PARCIALMENTE_ENTREGADA"))
                .andExpect(jsonPath("$.fechaDespacho").isNotEmpty())
                .andExpect(jsonPath("$.medicamentos[0].entregado").value(true))
                .andExpect(jsonPath("$.medicamentos[1].entregado").value(false))
                .andExpect(jsonPath("$.medicamentos[1].fechaEntrega").isEmpty());
        // Otra entrega que todavía no completa la receta la deja igual.
        despachar(receta.getId(), ids[1]).andExpect(jsonPath("$.estado").value("PARCIALMENTE_ENTREGADA"));

        // Lo ya entregado no se entrega dos veces, y el 409 no deja nada a medias.
        despachar(receta.getId(), ids[0], ids[2])
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("«Losartán» ya se entregó"));
        assertThat(entregados(visita)).containsExactly(true, true, false);
        // Sin entregas, después del primer despacho, no cambiaría nada.
        despachar(receta.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.entregados").value("Marque al menos un medicamento entregado"));

        despachar(receta.getId(), ids[2])
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADA"));
    }

    /** CU-005, E.1: no hay existencias y la farmacia lo deja anotado; cuando llegan, se entrega. */
    @Test
    @DisplayName("Un despacho sin entregas deja la receta en Medicamento Agotado, sin fecha y todavía por despachar; cuando hay existencias, se entrega")
    void sinExistenciasQuedaAgotada() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Cita cita = citaEl(especialista, HOY.atTime(11, 0));
        long visita = idDe(iniciar(cita.getId(), especialista));
        recetar(visita, especialista, null, medicamento("Insulina glargina", "10 UI", 1, "Cada noche, subcutánea"))
                .andExpect(status().isCreated());
        Receta receta = recetaRepository.findByVisitaId(visita).orElseThrow();

        despachar(receta.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("MEDICAMENTO_AGOTADO"))
                .andExpect(jsonPath("$.fechaDespacho").isEmpty())
                .andExpect(jsonPath("$.medicamentos[0].entregado").value(false));
        // YS-131: lo que no se entrega no se cobra (CU-005, E.2).
        assertThat(cargosDeFarmacia(visita)).isEmpty();
        // Sigue entre las que la farmacia tiene que despachar.
        assertThat(idsDeRecetas(dpiDe(cita), "PENDIENTE_DESPACHO", "PARCIALMENTE_ENTREGADA", "MEDICAMENTO_AGOTADO"))
                .containsExactly(receta.getId().intValue());
        despachar(receta.getId()).andExpect(status().isBadRequest());

        despachar(receta.getId(), idsDe(receta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADA"))
                .andExpect(jsonPath("$.fechaDespacho").isNotEmpty());
        assertThat(cargosDeFarmacia(visita)).hasSize(1);
    }

    @Test
    @DisplayName("Un medicamento de otra receta o repetido es 400 en su fila; sin la lista, 400; una receta que no existe, 404; y nada se entrega")
    void despachoInvalido() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Especialista otro = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(11, 30)).getId(), especialista));
        long otraVisita = idDe(iniciar(citaEl(otro, HOY.atTime(11, 30)).getId(), otro));
        recetar(visita, especialista, null,
                medicamento("Losartán", "50 mg", 30, "Cada 24 horas"),
                medicamento("Amlodipino", "5 mg", 30, "Cada 24 horas")).andExpect(status().isCreated());
        recetar(otraVisita, otro, null, medicamento("Omeprazol", "20 mg", 14, "En ayunas"))
                .andExpect(status().isCreated());
        Receta receta = recetaRepository.findByVisitaId(visita).orElseThrow();
        Long[] ids = idsDe(receta);
        Long ajeno = idsDe(recetaRepository.findByVisitaId(otraVisita).orElseThrow())[0];

        despachar(receta.getId(), ids[0], ajeno)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['entregados[1].id']")
                        .value("Ese medicamento no está en la receta; vuelva a abrirla"));
        despachar(receta.getId(), ids[1], ids[1])
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['entregados[1].id']").value("«Amlodipino» está repetido"));
        despacharCon(receta.getId(), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.entregados").value("Indique qué medicamentos se entregaron"));
        despacharCon(receta.getId(), "{\"entregados\": [{}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['entregados[0].id']").value("Indique el medicamento"));
        despachar(999_999L, ids[0]).andExpect(status().isNotFound());

        assertThat(entregados(visita)).containsExactly(false, false);
        assertThat(recetaRepository.findByVisitaId(visita).orElseThrow().getEstado())
                .isEqualTo(EstadoReceta.PENDIENTE_DESPACHO);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = "FARMACIA", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Los otros seis roles no despachan recetas: 403")
    void soloLaFarmaciaDespacha(RolNombre rol) throws Exception {
        // El rol se revisa antes de buscar la receta: no hace falta que exista.
        mvc.perform(post("/api/recetas/{id}/despacho", 999_999)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"entregados\": []}"))
                .andExpect(status().isForbidden());
    }

    /** El módulo "Despacho de medicamentos" (decidido el 19/10). */
    @Test
    @DisplayName("El historial de despachos trae las recetas con entregas, del despacho más reciente al más antiguo")
    void elHistorialDeDespachos() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Cita primera = citaEl(especialista, HOY.atTime(8, 0));
        Cita segunda = citaEl(especialista, HOY.atTime(8, 30), primera.getSolicitud().getInterno().getId());
        long visitaPrimera = idDe(iniciar(primera.getId(), especialista));
        recetar(visitaPrimera, especialista, null, medicamento("Losartán", "50 mg", 30, "Cada 24 horas"))
                .andExpect(status().isCreated());
        cerrar(visitaPrimera, "Hipertensión arterial", HOY.atTime(8, 0));
        long visitaSegunda = idDe(iniciar(segunda.getId(), especialista));
        recetar(visitaSegunda, especialista, null,
                medicamento("Metformina", "850 mg", 60, "Cada 12 horas"),
                medicamento("Glibenclamida", "5 mg", 30, "Antes del desayuno")).andExpect(status().isCreated());
        Receta deLaPrimera = recetaRepository.findByVisitaId(visitaPrimera).orElseThrow();
        Receta deLaSegunda = recetaRepository.findByVisitaId(visitaSegunda).orElseThrow();

        // Primero una entrega parcial de la segunda y después la primera completa.
        despachar(deLaSegunda.getId(), idsDe(deLaSegunda)[0]).andExpect(status().isOk());
        despachar(deLaPrimera.getId(), idsDe(deLaPrimera)).andExpect(status().isOk());

        String respuesta = mvc.perform(get("/api/recetas").param("paciente", dpiDe(primera))
                .param("estado", "PARCIALMENTE_ENTREGADA", "ENTREGADA").param("sort", "fechaDespacho,desc")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.FARMACIA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Integer> historial = JsonPath.read(respuesta, "$.content[*].id");
        assertThat(historial).containsExactly(deLaPrimera.getId().intValue(), deLaSegunda.getId().intValue());
    }

    // ---- YS-131: el costo de los medicamentos (HU-17) ----

    @Test
    @DisplayName("Cada medicamento entregado queda con su costo y como un cargo de farmacia de la visita; lo pendiente no se cobra y nada se cobra dos veces")
    void elCostoDeCadaMedicamentoEsUnCargo() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Cita cita = citaEl(especialista, HOY.atTime(9, 0));
        long visita = idDe(iniciar(cita.getId(), especialista));
        recetar(visita, especialista, null,
                medicamento("Losartán", "50 mg", 30, "Cada 24 horas"),
                medicamento("Amlodipino", "5 mg", 30, "Cada 24 horas"),
                medicamento("Metformina", "850 mg", 60, "Cada 12 horas")).andExpect(status().isCreated());
        Receta receta = recetaRepository.findByVisitaId(visita).orElseThrow();
        Long[] ids = idsDe(receta);

        despacharEntregas(receta.getId(), entrega(ids[0], "35"), entrega(ids[1], "20,50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PARCIALMENTE_ENTREGADA"))
                // RecetaResponse también la recibe el especialista: no lleva costos.
                .andExpect(jsonPath("$.medicamentos[0].costo").doesNotExist());

        List<CargoServicio> cargos = cargosDeFarmacia(visita);
        assertThat(cargos).extracting(CargoServicio::getMontoOriginal)
                .containsExactly(new BigDecimal("35.00"), new BigDecimal("20.50"));
        assertThat(cargos).allSatisfy(cargo -> {
            assertThat(cargo.getDescuento()).isEqualByComparingTo("0");
            assertThat(cargo.getMontoNeto()).isEqualByComparingTo(cargo.getMontoOriginal());
            assertThat(cargo.getInterno().getId()).isEqualTo(cita.getSolicitud().getInterno().getId());
        });
        costosDe(receta.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entregados.length()").value(2))
                .andExpect(jsonPath("$.entregados[0].id").value(ids[0]))
                .andExpect(jsonPath("$.entregados[0].costo").value(35.00))
                .andExpect(jsonPath("$.entregados[1].costo").value(20.50))
                .andExpect(jsonPath("$.total").value(55.50));

        // El resto llega en otro despacho: un cargo más por lo nuevo, ninguno por lo ya cobrado.
        despacharEntregas(receta.getId(), entrega(ids[2], "40"))
                .andExpect(jsonPath("$.estado").value("ENTREGADA"));
        assertThat(cargosDeFarmacia(visita)).hasSize(3);
        costosDe(receta.getId()).andExpect(jsonPath("$.total").value(95.50));
        costosDe(999_999L).andExpect(status().isNotFound());

        // Ni siquiera en su visita el especialista ve lo que costaron.
        mvc.perform(get("/api/visitas/{id}/receta", visita).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicamentos[0].costo").doesNotExist());
    }

    @Test
    @DisplayName("El costo de cada medicamento entregado es obligatorio y mayor que cero: 400 en su fila, y no se entrega ni se cobra nada")
    void costoInvalidoDelMedicamento() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(9, 30)).getId(), especialista));
        recetar(visita, especialista, null,
                medicamento("Losartán", "50 mg", 30, "Cada 24 horas"),
                medicamento("Amlodipino", "5 mg", 30, "Cada 24 horas")).andExpect(status().isCreated());
        Receta receta = recetaRepository.findByVisitaId(visita).orElseThrow();
        Long[] ids = idsDe(receta);

        Map<String, String> casos = new LinkedHashMap<>();
        casos.put("", "Indique el costo del medicamento");
        casos.put("0", "El costo tiene que ser mayor que cero");
        casos.put("0.00", "El costo tiene que ser mayor que cero");
        casos.put("-5", "El costo tiene que ser mayor que cero");
        casos.put("cinco", "Escriba un monto como 75 o 75.50");
        casos.put("12.345", "Escriba un monto como 75 o 75.50");
        casos.put("1e3", "Escriba un monto como 75 o 75.50");
        for (Map.Entry<String, String> caso : casos.entrySet()) {
            despacharEntregas(receta.getId(), entrega(ids[0], "10.00"), entrega(ids[1], caso.getKey()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores['entregados[1].costo']").value(caso.getValue()));
        }
        // Sin el campo, igual que vacío.
        despacharCon(receta.getId(), "{\"entregados\": [{\"id\": %d}]}".formatted(ids[0]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['entregados[0].costo']").value("Indique el costo del medicamento"));

        assertThat(entregados(visita)).containsExactly(false, false);
        assertThat(cargosDeFarmacia(visita)).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = "FARMACIA", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Los otros seis roles no ven lo que cobró la farmacia: 403")
    void soloLaFarmaciaVeLosCostos(RolNombre rol) throws Exception {
        mvc.perform(get("/api/recetas/{id}/costos", 999_999).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol)))
                .andExpect(status().isForbidden());
    }

    // ---- YS-135: la cuenta del familiar (HU-18) ----

    @Test
    @DisplayName("Consolidar el mes suma la cuota base y los cargos de consulta, laboratorio y farmacia; consolidar otra vez recalcula sin duplicar")
    void consolidaLaCuentaDelMes() throws Exception {
        tarifaDe(6, "410.00");
        Especialista especialista = nuevoEspecialista(6);
        Cita cita = citaEl(especialista, HOY.atTime(8, 0));
        Long interno = cita.getSolicitud().getInterno().getId();
        cambiarCuota(interno, "1500")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuotaMensual").value(1500.00));
        long visita = conLosTresCargos(cita, especialista, "Glucosa en ayunas", "120.00", "Losartán", "35.50");

        String periodo = YearMonth.now().toString();
        String filtro = "$[?(@.interno.id == %d)]";
        consolidar(periodo)
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtro + ".periodo", interno).value(periodo))
                .andExpect(jsonPath(filtro + ".familiar.nombreCompleto", interno).value("Familiar de prueba"))
                .andExpect(jsonPath(filtro + ".cuotaBase", interno).value(1500.00))
                .andExpect(jsonPath(filtro + ".totalServicios", interno).value(565.50))
                .andExpect(jsonPath(filtro + ".totalDescuentos", interno).value(0.00))
                .andExpect(jsonPath(filtro + ".totalAPagar", interno).value(2065.50))
                .andExpect(jsonPath(filtro + ".saldo", interno).value(2065.50))
                .andExpect(jsonPath(filtro + ".estado", interno).value("PENDIENTE_PAGO"));
        List<CuentaMensual> cuentas = cuentaRepository.findByInternoId(interno);
        assertThat(cuentas).hasSize(1);
        assertThat(cargoRepository.findByVisitaIdOrderByIdAsc(visita)).hasSize(3)
                .allSatisfy(cargo -> assertThat(cargo.getCuenta().getId()).isEqualTo(cuentas.get(0).getId()));

        // Otra vez, después de cambiar la cuota: la misma cuenta, recalculada, y los cargos sin repetirse.
        cambiarCuota(interno, "1600").andExpect(status().isOk());
        consolidar(periodo)
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtro + ".id", interno).value(cuentas.get(0).getId().intValue()))
                .andExpect(jsonPath(filtro + ".totalServicios", interno).value(565.50))
                .andExpect(jsonPath(filtro + ".totalAPagar", interno).value(2165.50));
        assertThat(cuentaRepository.findByInternoId(interno)).hasSize(1);

        mvc.perform(get("/api/cuentas").param("periodo", periodo)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtro + ".totalAPagar", interno).value(2165.50));
    }

    /** RNF-03 y la sección 4.9 del diseño: lo que se cobra, sin decir de qué. */
    @Test
    @DisplayName("El estado de cuenta desglosa por servicio y por rango de fechas, sin especialidad, examen, medicamento ni diagnóstico")
    void elEstadoDeCuentaPorRango() throws Exception {
        tarifaDe(6, "410.00");
        Especialista especialista = nuevoEspecialista(6);
        Cita cita = citaEl(especialista, HOY.atTime(8, 30));
        Long interno = cita.getSolicitud().getInterno().getId();
        cambiarCuota(interno, "900").andExpect(status().isOk());
        conLosTresCargos(cita, especialista, "Perfil lipídico", "75", "Quetiapina", "48");
        LocalDate inicioDeMes = HOY.withDayOfMonth(1);
        String numero = solicitudRepository.findById(cita.getSolicitud().getId()).orElseThrow().getNumero();

        // Antes de consolidar: los cargos ya se ven, pero todavía no están en ninguna cuenta.
        estadoDeCuenta(interno, inicioDeMes, HOY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargos.length()").value(3))
                .andExpect(jsonPath("$.cargos[0].periodo").isEmpty())
                .andExpect(jsonPath("$.cargosSinConsolidar").value(3))
                .andExpect(jsonPath("$.cuentas").isEmpty());

        consolidar(YearMonth.now().toString()).andExpect(status().isOk());
        String cuerpo = estadoDeCuenta(interno, inicioDeMes, HOY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desglose[0].concepto").value("CUOTA"))
                .andExpect(jsonPath("$.desglose[0].neto").value(900.00))
                .andExpect(jsonPath("$.desglose[?(@.concepto == 'CONSULTA')].neto").value(410.00))
                .andExpect(jsonPath("$.desglose[?(@.concepto == 'LABORATORIO')].neto").value(75.00))
                .andExpect(jsonPath("$.desglose[?(@.concepto == 'FARMACIA')].cantidad").value(1))
                .andExpect(jsonPath("$.desglose[?(@.concepto == 'FARMACIA')].neto").value(48.00))
                .andExpect(jsonPath("$.cargos[*].numeroSolicitud").value(contains(numero, numero, numero)))
                .andExpect(jsonPath("$.cargos[0].periodo").value(YearMonth.now().toString()))
                .andExpect(jsonPath("$.cargosSinConsolidar").value(0))
                .andExpect(jsonPath("$.totalAPagar").value(1433.00))
                .andExpect(jsonPath("$.saldoTotal").value(1433.00))
                .andExpect(jsonPath("$.cuentas[0].estado").value("PENDIENTE_PAGO"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String especialidad = especialidadRepository.findById(idEspecialidad(6)).orElseThrow().getNombre();
        assertThat(cuerpo).doesNotContain("Perfil lipídico", "Quetiapina", "Hipertensión", especialidad);

        // Un rango que no toca este mes no trae cargos ni cuotas; lo que debe en total no cambia.
        estadoDeCuenta(interno, HOY.minusYears(1).withDayOfMonth(1), HOY.minusYears(1).withDayOfMonth(28))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargos").isEmpty())
                .andExpect(jsonPath("$.cuentas").isEmpty())
                .andExpect(jsonPath("$.totalAPagar").value(0.00))
                .andExpect(jsonPath("$.saldoTotal").value(1433.00));
        estadoDeCuenta(interno, HOY, HOY.minusDays(1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.hasta").value("La fecha final no puede ser anterior a la inicial"));
        estadoDeCuenta(999_999L, inicioDeMes, HOY).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tiene cuenta quien tiene cuota o recibió servicios en el mes; sin ninguna de las dos, no")
    void quienTieneCuenta() throws Exception {
        tarifaDe(6, "410.00");
        Especialista especialista = nuevoEspecialista(6);
        Cita cita = citaEl(especialista, HOY.atTime(9, 0));
        Long conServicios = cita.getSolicitud().getInterno().getId();
        long visita = idDe(iniciar(cita.getId(), especialista));
        diagnosticar(visita, especialista, "Control", null).andExpect(status().isOk());
        finalizar(visita, especialista).andExpect(status().isOk());
        long sinNada = crearInterno(tokenDe(RolNombre.MEDICO_GENERAL));

        consolidar(YearMonth.now().toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.interno.id == %d)].cuotaBase", conServicios).value(0.00))
                .andExpect(jsonPath("$[?(@.interno.id == %d)].totalAPagar", conServicios).value(410.00));
        assertThat(cuentaRepository.findByInternoId(sinNada)).isEmpty();
    }

    @Test
    @DisplayName("La cuota la fija el Administrador del Asilo, mayor o igual que cero; el Médico General ya no la cambia al editar al interno")
    void laCuotaEsDelAdministrador() throws Exception {
        long interno = crearInterno(tokenDe(RolNombre.MEDICO_GENERAL));

        cambiarCuota(interno, "-5")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cuotaMensual").value("La cuota mensual no puede ser negativa"));
        cambiarCuota(interno, "null")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cuotaMensual").value("Indique la cuota mensual"));
        cambiarCuota(interno, "12.345").andExpect(status().isBadRequest());
        cambiarCuota(999_999L, "100").andExpect(status().isNotFound());
        cambiarCuota(interno, "0").andExpect(status().isOk()).andExpect(jsonPath("$.cuotaMensual").value(0.00));
        cambiarCuota(interno, "1250.5").andExpect(status().isOk()).andExpect(jsonPath("$.cuotaMensual").value(1250.50));

        // El Médico General edita al interno con la cuota que tenía en pantalla: ya no la pisa.
        String token = tokenDe(RolNombre.MEDICO_GENERAL);
        String expediente = mvc.perform(get("/api/internos/{id}", interno).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String edicion = """
                {"nombreCompleto": "Interno con visita",
                 "dpi": "%s",
                 "fechaNacimiento": "1943-02-14",
                 "fechaIngreso": "2023-05-02",
                 "cuotaMensual": 10,
                 "familiarResponsable": {
                   "nombreCompleto": "Familiar de prueba",
                   "parentesco": "Hija",
                   "telefono": "5555 0001"}}
                """.formatted((String) JsonPath.read(expediente, "$.dpi"));
        mvc.perform(put("/api/internos/{id}", interno)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(edicion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuotaMensual").value(1250.50));
    }

    @Test
    @DisplayName("Un mes que todavía no empieza no se consolida, y un período mal escrito o ausente es 400")
    void periodoInvalido() throws Exception {
        consolidar(YearMonth.now().plusMonths(1).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.periodo").value("Ese mes todavía no empieza: no hay nada que consolidar"));
        consolidar("octubre").andExpect(status().isBadRequest());
        mvc.perform(post("/api/cuentas/consolidar")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = "ADMINISTRADOR_ASILO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Los otros seis roles no ven ni consolidan cuentas, ni fijan la cuota: 403")
    void soloElAdministradorDelAsiloCobra(RolNombre rol) throws Exception {
        String token = "Bearer " + tokenDe(rol);
        mvc.perform(get("/api/cuentas").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/cuentas/consolidar").param("periodo", YearMonth.now().toString())
                .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/internos/{id}/estado-cuenta", 999_999).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/internos/{id}/cuota", 999_999)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cuotaMensual\": 100}"))
                .andExpect(status().isForbidden());
    }

    // ---- YS-138: los descuentos de la fundación (HU-19) ----

    /**
     * Decidido el 20/10: el descuento va por la fecha del servicio y se aplica
     * al consolidar. Las políticas son de todo el sistema, así que la prueba
     * las borra al terminar: la base es compartida y las demás cuentan sin
     * descuento.
     */
    @Test
    @DisplayName("Al consolidar, cada cargo toma el descuento vigente en su fecha: porcentaje, monto fijo sin neto negativo, y nada sin política")
    void elDescuentoSeAplicaAlConsolidar() throws Exception {
        try {
            tarifaDe(6, "410.00");
            Especialista especialista = nuevoEspecialista(6);
            Cita cita = citaEl(especialista, HOY.atTime(9, 30));
            Long interno = cita.getSolicitud().getInterno().getId();
            long visita = conLosTresCargos(cita, especialista, "Creatinina", "120.00", "Omeprazol", "35.50");
            // Los cargos nacieron antes de que hubiera política: sin descuento.
            assertThat(cargoRepository.findByVisitaIdOrderByIdAsc(visita))
                    .allSatisfy(cargo -> assertThat(cargo.getDescuento()).isEqualByComparingTo("0"));

            LocalDate inicioDeMes = HOY.withDayOfMonth(1);
            definirDescuento("CONSULTA", "PORCENTAJE", "20", inicioDeMes)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.vigente").value(true))
                    .andExpect(jsonPath("$.registradaPor").value("Persona de prueba"));
            // Más que el costo del medicamento: el cargo queda en cero, no negativo.
            definirDescuento("FARMACIA", "MONTO_FIJO", "50", inicioDeMes).andExpect(status().isOk());
            // Uno que todavía no empieza no toca los cargos de hoy.
            definirDescuento("LABORATORIO", "PORCENTAJE", "90", HOY.plusMonths(1)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.vigente").value(false));

            String filtro = "$[?(@.interno.id == %d)]";
            consolidar(YearMonth.now().toString())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(filtro + ".totalServicios", interno).value(565.50))
                    .andExpect(jsonPath(filtro + ".totalDescuentos", interno).value(117.50))
                    .andExpect(jsonPath(filtro + ".totalAPagar", interno).value(448.00));
            Map<TipoServicio, CargoServicio> porTipo = new LinkedHashMap<>();
            cargoRepository.findByVisitaIdOrderByIdAsc(visita).forEach(cargo -> porTipo.put(cargo.getTipo(), cargo));
            assertThat(porTipo.get(TipoServicio.CONSULTA).getDescuento()).isEqualByComparingTo("82.00");
            assertThat(porTipo.get(TipoServicio.CONSULTA).getMontoNeto()).isEqualByComparingTo("328.00");
            assertThat(porTipo.get(TipoServicio.FARMACIA).getDescuento()).isEqualByComparingTo("35.50");
            assertThat(porTipo.get(TipoServicio.FARMACIA).getMontoNeto()).isEqualByComparingTo("0.00");
            assertThat(porTipo.get(TipoServicio.LABORATORIO).getDescuento()).isEqualByComparingTo("0.00");

            // El estado de cuenta muestra el desglose entre original y descuento (criterio de HU-19).
            estadoDeCuenta(interno, inicioDeMes, HOY)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.desglose[?(@.concepto == 'CONSULTA')].original").value(410.00))
                    .andExpect(jsonPath("$.desglose[?(@.concepto == 'CONSULTA')].descuento").value(82.00))
                    .andExpect(jsonPath("$.desglose[?(@.concepto == 'CONSULTA')].neto").value(328.00))
                    .andExpect(jsonPath("$.totalDescuentos").value(117.50));

            // Un cargo que nace con la política ya definida la toma al nacer.
            Cita otra = citaEl(especialista, HOY.atTime(10, 0));
            long segunda = idDe(iniciar(otra.getId(), especialista));
            diagnosticar(segunda, especialista, "Control", null).andExpect(status().isOk());
            finalizar(segunda, especialista).andExpect(status().isOk());
            assertThat(cargoRepository.findByVisitaIdOrderByIdAsc(segunda).get(0).getMontoNeto())
                    .isEqualByComparingTo("328.00");
        } finally {
            politicaRepository.deleteAll();
        }
    }

    @Test
    @DisplayName("La política se valida, se versiona por fecha y se corrige definiendo la misma fecha otra vez")
    void lasPoliticasSeVersionan() throws Exception {
        try {
            LocalDate desde = HOY.withDayOfMonth(1);
            definirDescuento("CONSULTA", "PORCENTAJE", "150", desde)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.valor").value("Un porcentaje no puede pasar de 100"));
            definirDescuento("CONSULTA", "MONTO_FIJO", "-1", desde)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.valor").value("El descuento no puede ser negativo"));
            mvc.perform(put("/api/politicas-descuento/{tipo}", "CONSULTA")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_FUNDACION))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"tipoDescuento\": \"PORCENTAJE\", \"valor\": 10}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.vigenteDesde").value("Indique desde qué fecha rige"));
            // La cuota no lleva descuento: no es un tipo de servicio.
            definirDescuento("CUOTA", "PORCENTAJE", "10", desde).andExpect(status().isBadRequest());

            definirDescuento("CONSULTA", "PORCENTAJE", "15", desde.minusMonths(1)).andExpect(status().isOk());
            definirDescuento("CONSULTA", "PORCENTAJE", "20", desde).andExpect(status().isOk());
            // La misma fecha otra vez corrige esa versión, no agrega otra.
            definirDescuento("CONSULTA", "MONTO_FIJO", "25", desde).andExpect(status().isOk());
            assertThat(politicaRepository.findByTipoServicioOrderByVigenteDesdeDesc(TipoServicio.CONSULTA)).hasSize(2);

            mvc.perform(get("/api/politicas-descuento")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].tipoDescuento").value("MONTO_FIJO"))
                    .andExpect(jsonPath("$[0].valor").value(25.00))
                    .andExpect(jsonPath("$[0].vigente").value(true))
                    .andExpect(jsonPath("$[1].valor").value(15.00))
                    .andExpect(jsonPath("$[1].vigente").value(false));
        } finally {
            politicaRepository.deleteAll();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = "ADMINISTRADOR_FUNDACION", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Solo el Administrador de la Fundación define los descuentos: los otros seis, el del Asilo incluido, reciben 403")
    void soloLaFundacionDefineDescuentos(RolNombre rol) throws Exception {
        mvc.perform(put("/api/politicas-descuento/{tipo}", "CONSULTA")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoDescuento\": \"PORCENTAJE\", \"valor\": 10, \"vigenteDesde\": \"%s\"}".formatted(HOY)))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = { "ADMINISTRADOR_FUNDACION", "ADMINISTRADOR_ASILO" },
            mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Los descuentos los consultan los dos administradores; los roles clínicos reciben 403")
    void soloLosAdministradoresVenLosDescuentos(RolNombre rol) throws Exception {
        mvc.perform(get("/api/politicas-descuento").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol)))
                .andExpect(status().isForbidden());
    }

    // ---- YS-178: los pagos del familiar (HU-34) ----

    @Test
    @DisplayName("Un pago parcial se descuenta del saldo y deja la cuenta en Pago Parcial; el que completa el total la cancela")
    void pagoParcialYDespuesElTotal() throws Exception {
        long interno = crearInterno(tokenDe(RolNombre.MEDICO_GENERAL));
        cambiarCuota(interno, "1000").andExpect(status().isOk());
        long cuenta = cuentaDelMes(interno);

        pagar(cuenta, "400", HOY, "EFECTIVO")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuenta.totalAPagar").value(1000.00))
                .andExpect(jsonPath("$.cuenta.totalPagado").value(400.00))
                .andExpect(jsonPath("$.cuenta.saldo").value(600.00))
                .andExpect(jsonPath("$.cuenta.estado").value("PAGO_PARCIAL"))
                .andExpect(jsonPath("$.pagos.length()").value(1))
                .andExpect(jsonPath("$.pagos[0].monto").value(400.00))
                .andExpect(jsonPath("$.pagos[0].formaPago").value("EFECTIVO"))
                .andExpect(jsonPath("$.pagos[0].registradoPor").value("Persona de prueba"))
                .andExpect(jsonPath("$.pagos[0].anulado").value(false));
        // El resto, de ayer y con referencia: el historial va del más reciente al más antiguo.
        pagarCon(cuenta, """
                {"monto": 600.00, "fecha": "%s", "formaPago": "TRANSFERENCIA", "referencia": "  BI-2026-0042  "}
                """.formatted(HOY.minusDays(1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuenta.totalPagado").value(1000.00))
                .andExpect(jsonPath("$.cuenta.saldo").value(0.00))
                .andExpect(jsonPath("$.cuenta.estado").value("COMPLETAMENTE_CANCELADA"))
                .andExpect(jsonPath("$.pagos.length()").value(2))
                .andExpect(jsonPath("$.pagos[0].monto").value(400.00))
                .andExpect(jsonPath("$.pagos[1].monto").value(600.00))
                .andExpect(jsonPath("$.pagos[1].referencia").value("BI-2026-0042"));

        // Pagada completa, no recibe más pagos.
        pagar(cuenta, "1", HOY, "EFECTIVO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Esta cuenta ya está pagada completa"));

        pagos(cuenta)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuenta.estado").value("COMPLETAMENTE_CANCELADA"))
                .andExpect(jsonPath("$.pagos.length()").value(2));
        mvc.perform(get("/api/cuentas").param("periodo", YearMonth.now().toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].totalPagado", cuenta).value(1000.00))
                .andExpect(jsonPath("$[?(@.id == %d)].saldo", cuenta).value(0.00));
        estadoDeCuenta(interno, HOY.withDayOfMonth(1), HOY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuentas[0].totalPagado").value(1000.00))
                .andExpect(jsonPath("$.saldoTotal").value(0.00));
    }

    @Test
    @DisplayName("El monto tiene que ser mayor que cero y no pasar del saldo; la fecha no puede ser futura y la forma de pago es obligatoria")
    void pagoInvalido() throws Exception {
        long interno = crearInterno(tokenDe(RolNombre.MEDICO_GENERAL));
        cambiarCuota(interno, "500").andExpect(status().isOk());
        long cuenta = cuentaDelMes(interno);

        pagar(cuenta, "0", HOY, "EFECTIVO")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").value("El monto tiene que ser mayor que cero"));
        pagar(cuenta, "-5", HOY, "EFECTIVO")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").value("El monto tiene que ser mayor que cero"));
        pagar(cuenta, "12.345", HOY, "EFECTIVO")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").value("El monto admite hasta dos decimales"));
        pagar(cuenta, "500.01", HOY, "EFECTIVO")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").value("El pago no puede pasar del saldo pendiente, Q500.00"));
        pagar(cuenta, "100", HOY.plusDays(1), "EFECTIVO")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fecha").value("La fecha del pago no puede estar en el futuro"));
        pagarCon(cuenta, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").value("Indique el monto del pago"))
                .andExpect(jsonPath("$.errores.fecha").value("Indique la fecha del pago"))
                .andExpect(jsonPath("$.errores.formaPago").value("Elija la forma de pago"));
        pagar(cuenta, "100", HOY, "BITCOIN").andExpect(status().isBadRequest());
        pagar(999_999L, "100", HOY, "EFECTIVO").andExpect(status().isNotFound());
        mvc.perform(get("/api/cuentas/{id}/pagos", cuenta)).andExpect(status().isUnauthorized());

        // Nada de eso quedó registrado.
        assertThat(pagoRepository.findByCuentaIdOrderByFechaDescIdDesc(cuenta)).isEmpty();
        assertThat(cuentaRepository.findById(cuenta).orElseThrow().getSaldo()).isEqualByComparingTo("500.00");
    }

    /** Decidido el 20/10: el pago mal registrado se anula con su motivo, no se borra. */
    @Test
    @DisplayName("Anular un pago devuelve su monto al saldo y recalcula el estado; queda en el historial con el motivo y no se anula dos veces")
    void anularUnPago() throws Exception {
        long interno = crearInterno(tokenDe(RolNombre.MEDICO_GENERAL));
        cambiarCuota(interno, "800").andExpect(status().isOk());
        long cuenta = cuentaDelMes(interno);
        String respuesta = pagar(cuenta, "800", HOY, "CHEQUE")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuenta.estado").value("COMPLETAMENTE_CANCELADA"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long pago = ((Number) JsonPath.read(respuesta, "$.pagos[0].id")).longValue();

        anular(pago, "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivo").value("Escriba el motivo de la anulación"));
        anular(pago, "  Se registró Q800 en lugar de Q80  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuenta.totalPagado").value(0.00))
                .andExpect(jsonPath("$.cuenta.saldo").value(800.00))
                .andExpect(jsonPath("$.cuenta.estado").value("PENDIENTE_PAGO"))
                .andExpect(jsonPath("$.pagos.length()").value(1))
                .andExpect(jsonPath("$.pagos[0].anulado").value(true))
                .andExpect(jsonPath("$.pagos[0].motivoAnulacion").value("Se registró Q800 en lugar de Q80"))
                .andExpect(jsonPath("$.pagos[0].anuladoPor").value("Persona de prueba"))
                .andExpect(jsonPath("$.pagos[0].fechaAnulacion").isNotEmpty());
        anular(pago, "Otra vez")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Este pago ya está anulado"));
        anular(999_999L, "No existe").andExpect(status().isNotFound());

        // Lo anulado ya no cuenta: el pago correcto deja la cuenta en Pago Parcial.
        pagar(cuenta, "80", HOY, "EFECTIVO")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuenta.totalPagado").value(80.00))
                .andExpect(jsonPath("$.cuenta.saldo").value(720.00))
                .andExpect(jsonPath("$.cuenta.estado").value("PAGO_PARCIAL"))
                .andExpect(jsonPath("$.pagos.length()").value(2));
        assertThat(pagoRepository.findByCuentaIdOrderByFechaDescIdDesc(cuenta)).hasSize(2);
    }

    /**
     * Decidido el 20/10: vence al final del mes siguiente. La cuenta se guarda
     * como estaba antes de vencer, sin mora: la mora la pone el calendario al
     * leerla, sin que nadie la toque.
     */
    @Test
    @DisplayName("Vencido el fin del mes siguiente, la cuenta con saldo está En Mora; un pago parcial no la saca, el del total sí")
    void laCuentaVencidaEstaEnMora() throws Exception {
        long interno = crearInterno(tokenDe(RolNombre.MEDICO_GENERAL));
        YearMonth vencido = YearMonth.now().minusMonths(2);
        long cuenta = cuentaAntesDeVencer(interno, vencido, "900.00");

        pagos(cuenta)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuenta.estado").value("EN_MORA"))
                .andExpect(jsonPath("$.cuenta.fechaVencimiento").value(vencido.plusMonths(1).atEndOfMonth().toString()))
                .andExpect(jsonPath("$.pagos").isEmpty());
        pagar(cuenta, "300", HOY, "DEPOSITO")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuenta.saldo").value(600.00))
                .andExpect(jsonPath("$.cuenta.estado").value("EN_MORA"));
        pagar(cuenta, "600", HOY, "TARJETA")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuenta.estado").value("COMPLETAMENTE_CANCELADA"));

        // La del mes pasado vence al final de este: todavía no está en mora.
        long reciente = cuentaAntesDeVencer(interno, YearMonth.now().minusMonths(1), "450.00");
        pagos(reciente)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuenta.estado").value("PENDIENTE_PAGO"))
                .andExpect(jsonPath("$.cuenta.fechaVencimiento").value(YearMonth.now().atEndOfMonth().toString()));
    }

    /**
     * Decidido el 20/10: al reconsolidar una cuenta con pagos se congela lo ya
     * cobrado. Las políticas son de todo el sistema: se borran al terminar.
     */
    @Test
    @DisplayName("Reconsolidar una cuenta con pagos congela lo cobrado y solo suma los cargos nuevos; la cancelada vuelve a Pago Parcial")
    void reconsolidarConPagosCongelaLoCobrado() throws Exception {
        try {
            tarifaDe(6, "410.00");
            Especialista especialista = nuevoEspecialista(6);
            Cita cita = citaEl(especialista, HOY.atTime(10, 30));
            Long interno = cita.getSolicitud().getInterno().getId();
            cambiarCuota(interno, "1000").andExpect(status().isOk());
            long visita = idDe(iniciar(cita.getId(), especialista));
            diagnosticar(visita, especialista, "Control", null).andExpect(status().isOk());
            finalizar(visita, especialista).andExpect(status().isOk());
            long cuenta = cuentaDelMes(interno);
            pagar(cuenta, "1410", HOY, "EFECTIVO")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.cuenta.estado").value("COMPLETAMENTE_CANCELADA"));

            // Después del pago: otra cuota, un descuento que ya regía y una consulta nueva.
            cambiarCuota(interno, "2000").andExpect(status().isOk());
            definirDescuento("CONSULTA", "PORCENTAJE", "50", HOY.withDayOfMonth(1)).andExpect(status().isOk());
            Cita otra = citaEl(especialista, HOY.atTime(11, 0), interno);
            long segunda = idDe(iniciar(otra.getId(), especialista));
            diagnosticar(segunda, especialista, "Control", null).andExpect(status().isOk());
            finalizar(segunda, especialista).andExpect(status().isOk());

            String filtro = "$[?(@.id == %d)]";
            consolidar(YearMonth.now().toString())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(filtro + ".cuotaBase", cuenta).value(1000.00))
                    .andExpect(jsonPath(filtro + ".totalServicios", cuenta).value(820.00))
                    .andExpect(jsonPath(filtro + ".totalDescuentos", cuenta).value(205.00))
                    .andExpect(jsonPath(filtro + ".totalAPagar", cuenta).value(1615.00))
                    .andExpect(jsonPath(filtro + ".totalPagado", cuenta).value(1410.00))
                    .andExpect(jsonPath(filtro + ".saldo", cuenta).value(205.00))
                    .andExpect(jsonPath(filtro + ".estado", cuenta).value("PAGO_PARCIAL"));
            // El cargo que ya estaba en la cuenta conserva su monto; el nuevo trae el descuento.
            assertThat(cargoRepository.findByVisitaIdOrderByIdAsc(visita).get(0).getMontoNeto())
                    .isEqualByComparingTo("410.00");
            assertThat(cargoRepository.findByVisitaIdOrderByIdAsc(segunda).get(0).getMontoNeto())
                    .isEqualByComparingTo("205.00");
        } finally {
            politicaRepository.deleteAll();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = "ADMINISTRADOR_ASILO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Los otros seis roles no registran, ni consultan, ni anulan pagos: 403")
    void soloElAdministradorDelAsiloRegistraPagos(RolNombre rol) throws Exception {
        String token = "Bearer " + tokenDe(rol);
        // Con cuerpos válidos: la validación corre antes que la seguridad del método.
        mvc.perform(post("/api/cuentas/{id}/pagos", 999_999)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"monto\": 100, \"fecha\": \"%s\", \"formaPago\": \"EFECTIVO\"}".formatted(HOY)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/cuentas/{id}/pagos", 999_999).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/pagos/{id}/anulacion", 999_999)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\": \"Error de digitación\"}"))
                .andExpect(status().isForbidden());
    }

    // ---- YS-141: las cuentas por pagar a la fundación (HU-20) ----

    /**
     * Decidido el 21/10: la deuda es el neto de los cargos del mes, siempre al
     * día. Los cargos se mueven a un mes propio de la prueba, porque el mes en
     * curso suma los de todas. La política de descuento se borra al terminar.
     */
    @Test
    @DisplayName("La deuda con la fundación es el neto de los cargos de cada mes y servicio, al día y sin nada clínico")
    void laDeudaSaleDeLosCargos() throws Exception {
        YearMonth mes = YearMonth.of(2001, 1);
        try {
            tarifaDe(6, "410.00");
            definirDescuento("CONSULTA", "PORCENTAJE", "20", LocalDate.of(2000, 1, 1)).andExpect(status().isOk());
            Especialista especialista = nuevoEspecialista(6);
            Cita cita = citaEl(especialista, HOY.atTime(8, 0));
            long visita = conLosTresCargos(cita, especialista, "Hemoglobina glicosilada (HbA1c)", "120.00", "Clozapina",
                    "35.50");
            cargosAlMes(visita, mes);

            String cuerpo = cuentasPorPagar()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(deuda(mes, "CONSULTA") + ".cantidadServicios").value(1))
                    // El neto: 410 con el 20 % de la fundación.
                    .andExpect(jsonPath(deuda(mes, "CONSULTA") + ".totalAdeudado").value(328.00))
                    .andExpect(jsonPath(deuda(mes, "CONSULTA") + ".totalPagado").value(0.00))
                    .andExpect(jsonPath(deuda(mes, "CONSULTA") + ".saldo").value(328.00))
                    .andExpect(jsonPath(deuda(mes, "CONSULTA") + ".estado").value("PENDIENTE"))
                    .andExpect(jsonPath(deuda(mes, "LABORATORIO") + ".totalAdeudado").value(120.00))
                    .andExpect(jsonPath(deuda(mes, "FARMACIA") + ".totalAdeudado").value(35.50))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(cuerpo).doesNotContain("Hemoglobina", "Clozapina", "Hipertensión", "Interno con visita");

            // Siempre al día: otra consulta de ese mes entra sola, sin consolidar nada.
            cargosAlMes(consultaFinalizada(especialista, HOY.atTime(9, 0)), mes);
            cuentasPorPagar()
                    .andExpect(jsonPath(deuda(mes, "CONSULTA") + ".cantidadServicios").value(2))
                    .andExpect(jsonPath(deuda(mes, "CONSULTA") + ".totalAdeudado").value(656.00));
        } finally {
            politicaRepository.deleteAll();
        }
    }

    @Test
    @DisplayName("Un pago parcial baja el saldo; el que lo completa deja la deuda Pagada, y un cargo nuevo del mes la regresa a Pendiente")
    void pagarALaFundacion() throws Exception {
        YearMonth mes = YearMonth.of(2001, 2);
        tarifaDe(6, "410.00");
        Especialista especialista = nuevoEspecialista(6);
        cargosAlMes(consultaFinalizada(especialista, HOY.atTime(8, 0)), mes);

        pagarALaFundacion(mes, "CONSULTA", "100", HOY, "EFECTIVO")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuenta.periodo").value("2001-02"))
                .andExpect(jsonPath("$.cuenta.tipoServicio").value("CONSULTA"))
                .andExpect(jsonPath("$.cuenta.totalAdeudado").value(410.00))
                .andExpect(jsonPath("$.cuenta.totalPagado").value(100.00))
                .andExpect(jsonPath("$.cuenta.saldo").value(310.00))
                .andExpect(jsonPath("$.cuenta.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.pagos.length()").value(1))
                .andExpect(jsonPath("$.pagos[0].formaPago").value("EFECTIVO"))
                .andExpect(jsonPath("$.pagos[0].registradoPor").value("Persona de prueba"));
        pagarALaFundacion(mes, "CONSULTA", "310", HOY, "TRANSFERENCIA")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuenta.saldo").value(0.00))
                .andExpect(jsonPath("$.cuenta.estado").value("PAGADA"))
                .andExpect(jsonPath("$.pagos.length()").value(2));
        pagarALaFundacion(mes, "CONSULTA", "1", HOY, "EFECTIVO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Esta deuda ya está pagada completa"));
        cuentasPorPagar()
                .andExpect(jsonPath(deuda(mes, "CONSULTA") + ".totalPagado").value(410.00))
                .andExpect(jsonPath(deuda(mes, "CONSULTA") + ".estado").value("PAGADA"));

        // Decidido el 21/10: si la deuda crece después de pagarla, vuelve a Pendiente.
        cargosAlMes(consultaFinalizada(especialista, HOY.atTime(9, 0)), mes);
        cuentasPorPagar()
                .andExpect(jsonPath(deuda(mes, "CONSULTA") + ".totalAdeudado").value(820.00))
                .andExpect(jsonPath(deuda(mes, "CONSULTA") + ".saldo").value(410.00))
                .andExpect(jsonPath(deuda(mes, "CONSULTA") + ".estado").value("PENDIENTE"));
        pagosDeLaFundacion(mes, "CONSULTA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuenta.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.pagos.length()").value(2));
    }

    @Test
    @DisplayName("El pago a la fundación tiene que ser mayor que cero y no pasar del saldo; sin deuda no hay nada que pagar")
    void pagoALaFundacionInvalido() throws Exception {
        YearMonth mes = YearMonth.of(2001, 3);
        tarifaDe(6, "410.00");
        cargosAlMes(consultaFinalizada(nuevoEspecialista(6), HOY.atTime(8, 0)), mes);

        pagarALaFundacion(mes, "CONSULTA", "0", HOY, "EFECTIVO")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").value("El monto tiene que ser mayor que cero"));
        pagarALaFundacion(mes, "CONSULTA", "410.01", HOY, "EFECTIVO")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").value("El pago no puede pasar del saldo pendiente, Q410.00"));
        pagarALaFundacion(mes, "CONSULTA", "100", HOY.plusDays(1), "EFECTIVO")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fecha").value("La fecha del pago no puede estar en el futuro"));
        mvc.perform(post("/api/cuentas-por-pagar/{periodo}/{tipo}/pagos", mes, "CONSULTA")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").value("Indique el monto del pago"))
                .andExpect(jsonPath("$.errores.fecha").value("Indique la fecha del pago"))
                .andExpect(jsonPath("$.errores.formaPago").value("Elija la forma de pago"));

        // Un mes sin servicios de ese tipo no debe nada.
        pagarALaFundacion(YearMonth.of(2001, 11), "LABORATORIO", "100", HOY, "EFECTIVO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No hay nada que pagarle a la fundación por ese servicio en ese mes"));
        pagosDeLaFundacion(YearMonth.of(2001, 11), "FARMACIA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuenta.totalAdeudado").value(0.00))
                .andExpect(jsonPath("$.pagos").isEmpty());
        // La cuota no es un servicio de la fundación, y el mes va como "2026-10".
        pagarALaFundacion(mes, "CUOTA", "100", HOY, "EFECTIVO").andExpect(status().isBadRequest());
        mvc.perform(get("/api/cuentas-por-pagar/{periodo}/{tipo}/pagos", "octubre", "CONSULTA")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/cuentas-por-pagar")).andExpect(status().isUnauthorized());

        // Nada de eso quedó registrado.
        cuentasPorPagar()
                .andExpect(jsonPath(deuda(mes, "CONSULTA") + ".totalPagado").value(0.00))
                .andExpect(jsonPath(deuda(mes, "CONSULTA") + ".saldo").value(410.00));
    }

    @Test
    @DisplayName("Anular un pago a la fundación devuelve su monto al saldo; queda en el historial con el motivo y no se anula dos veces")
    void anularPagoALaFundacion() throws Exception {
        YearMonth mes = YearMonth.of(2001, 4);
        tarifaDe(6, "410.00");
        cargosAlMes(consultaFinalizada(nuevoEspecialista(6), HOY.atTime(8, 0)), mes);
        String respuesta = pagarALaFundacion(mes, "CONSULTA", "410", HOY, "CHEQUE")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuenta.estado").value("PAGADA"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long pago = ((Number) JsonPath.read(respuesta, "$.pagos[0].id")).longValue();

        anularPagoDeLaFundacion(pago, "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivo").value("Escriba el motivo de la anulación"));
        anularPagoDeLaFundacion(pago, "  El cheque salió sin fondos  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuenta.totalPagado").value(0.00))
                .andExpect(jsonPath("$.cuenta.saldo").value(410.00))
                .andExpect(jsonPath("$.cuenta.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.pagos[0].anulado").value(true))
                .andExpect(jsonPath("$.pagos[0].motivoAnulacion").value("El cheque salió sin fondos"))
                .andExpect(jsonPath("$.pagos[0].anuladoPor").value("Persona de prueba"));
        anularPagoDeLaFundacion(pago, "Otra vez")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Este pago ya está anulado"));
        anularPagoDeLaFundacion(999_999L, "No existe").andExpect(status().isNotFound());

        // Lo anulado ya no cuenta.
        pagarALaFundacion(mes, "CONSULTA", "41", HOY, "EFECTIVO")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuenta.totalPagado").value(41.00))
                .andExpect(jsonPath("$.cuenta.saldo").value(369.00))
                .andExpect(jsonPath("$.pagos.length()").value(2));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = "ADMINISTRADOR_ASILO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Los otros seis roles no ven las cuentas por pagar, ni registran ni anulan pagos a la fundación: 403")
    void soloElAdministradorDelAsiloPagaALaFundacion(RolNombre rol) throws Exception {
        String token = "Bearer " + tokenDe(rol);
        mvc.perform(get("/api/cuentas-por-pagar").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/cuentas-por-pagar/{periodo}/{tipo}/pagos", "2001-05", "CONSULTA")
                .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
        // Con cuerpos válidos: la validación corre antes que la seguridad del método.
        mvc.perform(post("/api/cuentas-por-pagar/{periodo}/{tipo}/pagos", "2001-05", "CONSULTA")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"monto\": 100, \"fecha\": \"%s\", \"formaPago\": \"EFECTIVO\"}".formatted(HOY)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/pagos-fundacion/{id}/anulacion", 999_999)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\": \"Error de digitación\"}"))
                .andExpect(status().isForbidden());
    }

    // ---- YS-144: las citas pendientes de pago (HU-21) ----

    /**
     * Decidido el 21/10: una fila por cita, lo pagado de la cuenta repartido en
     * proporción, y las de un mes sin consolidar como Sin Pago. Se filtra por el
     * DPI del interno: la base es compartida y los internos de prueba se
     * llaman igual.
     */
    @Test
    @DisplayName("Una cita sin pagar sale Sin Pago, con un pago parcial lleva su parte en proporción y, pagada la cuenta, ya no aparece")
    void lasCitasPendientesSalenDeLosPagos() throws Exception {
        tarifaDe(6, "410.00");
        Especialista especialista = nuevoEspecialista(6);
        Cita cita = citaEl(especialista, HOY.atTime(8, 0));
        Long interno = cita.getSolicitud().getInterno().getId();
        String dpi = dpiDe(cita);
        // 410 de la consulta, 120 del examen y 35.50 del medicamento; con la cuota, la cuenta suma 1000.
        cambiarCuota(interno, "434.50").andExpect(status().isOk());
        conLosTresCargos(cita, especialista, "Glucosa en ayunas", "120.00", "Quetiapina", "35.50");
        String numero = solicitudRepository.findById(cita.getSolicitud().getId()).orElseThrow().getNumero();

        // Antes de consolidar: sin pago, y marcada.
        String cuerpo = citasPendientes(dpi, null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citas.length()").value(1))
                .andExpect(jsonPath("$.citas[0].numeroSolicitud").value(numero))
                .andExpect(jsonPath("$.citas[0].fecha").value(HOY.toString()))
                .andExpect(jsonPath("$.citas[0].paciente.id").value(interno.intValue()))
                .andExpect(jsonPath("$.citas[0].servicios[*].tipo").value(contains("CONSULTA", "LABORATORIO", "FARMACIA")))
                .andExpect(jsonPath("$.citas[0].servicios[*].cantidad").value(contains(1, 1, 1)))
                .andExpect(jsonPath("$.citas[0].montoTotal").value(565.50))
                .andExpect(jsonPath("$.citas[0].montoPagado").value(0.00))
                .andExpect(jsonPath("$.citas[0].saldo").value(565.50))
                .andExpect(jsonPath("$.citas[0].estado").value("SIN_PAGO"))
                .andExpect(jsonPath("$.citas[0].sinConsolidar").value(true))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String especialidad = especialidadRepository.findById(idEspecialidad(6)).orElseThrow().getNombre();
        assertThat(cuerpo).doesNotContain("Glucosa en ayunas", "Quetiapina", "Hipertensión", especialidad);

        long cuenta = cuentaDelMes(interno);
        citasPendientes(dpi, null, null)
                .andExpect(jsonPath("$.citas[0].estado").value("SIN_PAGO"))
                .andExpect(jsonPath("$.citas[0].sinConsolidar").value(false));

        // El 40 % de la cuenta: la cita lleva pagado el 40 % de lo suyo.
        pagar(cuenta, "400", HOY, "EFECTIVO").andExpect(status().isCreated());
        citasPendientes(dpi, null, null)
                .andExpect(jsonPath("$.citas[0].montoPagado").value(226.20))
                .andExpect(jsonPath("$.citas[0].saldo").value(339.30))
                .andExpect(jsonPath("$.citas[0].estado").value("PAGO_PARCIAL"))
                .andExpect(jsonPath("$.citas[0].vencida").value(false))
                .andExpect(jsonPath("$.saldoPendiente").value(339.30));

        // Vencida su cuenta, se marca: se lleva la cuenta a hace dos meses.
        CuentaMensual vencida = cuentaRepository.findById(cuenta).orElseThrow();
        vencida.setPeriodo(YearMonth.now().minusMonths(2).toString());
        cuentaRepository.save(vencida);
        citasPendientes(dpi, null, null)
                .andExpect(jsonPath("$.citas[0].estado").value("PAGO_PARCIAL"))
                .andExpect(jsonPath("$.citas[0].vencida").value(true));

        // Pagada completa, sale del listado.
        pagar(cuenta, "600", HOY, "EFECTIVO").andExpect(status().isCreated());
        citasPendientes(dpi, null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citas").isEmpty())
                .andExpect(jsonPath("$.saldoPendiente").value(0.00));
    }

    @Test
    @DisplayName("Las citas pendientes se filtran por rango de fechas y por paciente, sin mayúsculas ni tildes; un rango al revés es 400")
    void filtrosDeCitasPendientes() throws Exception {
        tarifaDe(6, "410.00");
        Especialista especialista = nuevoEspecialista(6);
        Cita anterior = citaEl(especialista, HOY.minusDays(3).atTime(8, 0));
        Long interno = anterior.getSolicitud().getInterno().getId();
        long primera = idDe(iniciar(anterior.getId(), especialista));
        diagnosticar(primera, especialista, "Control", null).andExpect(status().isOk());
        finalizar(primera, especialista).andExpect(status().isOk());
        long segunda = consultaFinalizada(especialista, HOY.atTime(9, 0), interno);
        String dpi = dpiDe(anterior);

        citasPendientes(dpi, null, null)
                .andExpect(jsonPath("$.citas[*].visitaId").value(contains((int) primera, (int) segunda)))
                .andExpect(jsonPath("$.montoTotal").value(820.00));
        citasPendientes(dpi, HOY, null)
                .andExpect(jsonPath("$.citas[*].visitaId").value(contains((int) segunda)));
        citasPendientes(dpi, null, HOY.minusDays(1))
                .andExpect(jsonPath("$.citas[*].visitaId").value(contains((int) primera)));
        citasPendientes(dpi, HOY.minusDays(3), HOY.minusDays(3))
                .andExpect(jsonPath("$.citas[*].visitaId").value(contains((int) primera)));

        // Por nombre, como lo escribe la gente: sin tildes ni mayúsculas.
        Interno conTildes = internoRepository.findById(interno).orElseThrow();
        conTildes.setNombreCompleto("Ñoño Ávila Pendiente " + dpi);
        internoRepository.save(conTildes);
        citasPendientes("nono avila pendiente " + dpi, null, null)
                .andExpect(jsonPath("$.citas.length()").value(2))
                .andExpect(jsonPath("$.citas[0].paciente.nombreCompleto").value("Ñoño Ávila Pendiente " + dpi));
        citasPendientes("nadie se llama así 123", null, null).andExpect(jsonPath("$.citas").isEmpty());

        citasPendientes(dpi, HOY, HOY.minusDays(1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.hasta").value("La fecha final no puede ser anterior a la inicial"));
        mvc.perform(get("/api/servicios-pendientes").param("desde", "ayer")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/servicios-pendientes")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = "ADMINISTRADOR_ASILO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Los otros seis roles no ven las citas pendientes de pago: 403")
    void soloElAdministradorDelAsiloVeLasPendientes(RolNombre rol) throws Exception {
        mvc.perform(get("/api/servicios-pendientes").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol)))
                .andExpect(status().isForbidden());
    }

    // ---- YS-107: el diagnóstico y el cierre de la visita (HU-09) ----

    @Test
    @DisplayName("El especialista guarda el diagnóstico y las observaciones, y puede corregirlos mientras la visita sigue activa")
    void guardaElDiagnostico() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(8, 0)).getId(), especialista));

        diagnosticar(visita, especialista, "Hipertensión", "Control en 15 días")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnostico").value("Hipertensión"))
                .andExpect(jsonPath("$.observaciones").value("Control en 15 días"))
                .andExpect(jsonPath("$.estado").value("ACTIVA"));

        // Se corrige: los espacios sobrantes se quitan y unas observaciones en
        // blanco quedan vacías, no como "   ".
        diagnosticar(visita, especialista, "  Hipertensión arterial estadio 1  ", "   ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnostico").value("Hipertensión arterial estadio 1"))
                .andExpect(jsonPath("$.observaciones").isEmpty());
    }

    @Test
    @DisplayName("Sin diagnóstico: 400 en el campo, y la visita no se puede finalizar")
    void sinDiagnosticoNoSeFinaliza() throws Exception {
        tarifaDe(6, "410.00");
        Especialista especialista = nuevoEspecialista(6);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(8, 30)).getId(), especialista));

        diagnosticar(visita, especialista, "   ", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.diagnostico").value("El diagnóstico es obligatorio"));
        finalizar(visita, especialista)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.diagnostico").value("Registre el diagnóstico antes de finalizar la visita"));

        assertThat(visitaRepository.findById(visita).orElseThrow().getEstado()).isEqualTo(EstadoVisita.ACTIVA);
        assertThat(cargoRepository.findByVisitaIdOrderByIdAsc(visita)).isEmpty();
    }

    @Test
    @DisplayName("Finalizar pasa la solicitud a Atendida y registra el cargo de la consulta con la tarifa de la especialidad")
    void finalizaLaVisita() throws Exception {
        tarifaDe(6, "410.00");
        Especialista especialista = nuevoEspecialista(6);
        Cita cita = citaEl(especialista, HOY.atTime(9, 0));
        long visita = idDe(iniciar(cita.getId(), especialista));
        diagnosticar(visita, especialista, "Gonartrosis bilateral", null).andExpect(status().isOk());

        finalizar(visita, especialista)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA"))
                .andExpect(jsonPath("$.estadoSolicitud").value("ATENDIDA"))
                .andExpect(jsonPath("$.fechaCierre").isNotEmpty())
                .andExpect(jsonPath("$.diagnostico").value("Gonartrosis bilateral"));

        assertThat(estadoDe(cita)).isEqualTo(EstadoSolicitud.ATENDIDA);
        List<CargoServicio> cargos = cargoRepository.findByVisitaIdOrderByIdAsc(visita);
        assertThat(cargos).hasSize(1);
        CargoServicio cargo = cargos.get(0);
        assertThat(cargo.getTipo()).isEqualTo(TipoServicio.CONSULTA);
        assertThat(cargo.getMontoOriginal()).isEqualByComparingTo("410.00");
        assertThat(cargo.getDescuento()).isEqualByComparingTo("0");
        assertThat(cargo.getMontoNeto()).isEqualByComparingTo("410.00");
        assertThat(cargo.getInterno().getId()).isEqualTo(cita.getSolicitud().getInterno().getId());
        assertThat(cargo.getFecha()).isEqualTo(HOY);
    }

    @Test
    @DisplayName("Una visita finalizada ya no acepta cambios: 409, y no se genera un segundo cargo")
    void finalizadaNoAceptaCambios() throws Exception {
        tarifaDe(6, "410.00");
        Especialista especialista = nuevoEspecialista(6);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(9, 30)).getId(), especialista));
        diagnosticar(visita, especialista, "Lumbalgia mecánica", null).andExpect(status().isOk());
        finalizar(visita, especialista).andExpect(status().isOk());

        diagnosticar(visita, especialista, "Otro diagnóstico", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La visita ya está finalizada y no admite cambios"));
        finalizar(visita, especialista).andExpect(status().isConflict());

        assertThat(cargoRepository.findByVisitaIdOrderByIdAsc(visita)).hasSize(1);
        assertThat(visitaRepository.findById(visita).orElseThrow().getDiagnostico()).isEqualTo("Lumbalgia mecánica");
    }

    @Test
    @DisplayName("Otro especialista no registra el diagnóstico ni finaliza una visita ajena: 403")
    void otroEspecialistaNoDiagnostica() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Especialista otro = nuevoEspecialista(0);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(10, 0)).getId(), especialista));

        diagnosticar(visita, otro, "Intento ajeno", null).andExpect(status().isForbidden());
        finalizar(visita, otro).andExpect(status().isForbidden());

        assertThat(visitaRepository.findById(visita).orElseThrow().getDiagnostico()).isNull();
    }

    /** Decidido el 10/10: sin tarifa no se finaliza, pero el diagnóstico no se pierde. */
    @Test
    @DisplayName("Sin tarifa de consulta la visita no se finaliza (409), pero el diagnóstico queda guardado")
    void sinTarifaNoSeFinaliza() throws Exception {
        sinTarifa(7);
        Especialista especialista = nuevoEspecialista(7);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(10, 30)).getId(), especialista));
        diagnosticar(visita, especialista, "Hiperplasia prostática", null).andExpect(status().isOk());

        finalizar(visita, especialista)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("todavía no tiene tarifa")));

        VisitaMedica guardada = visitaRepository.findById(visita).orElseThrow();
        assertThat(guardada.getEstado()).isEqualTo(EstadoVisita.ACTIVA);
        assertThat(guardada.getDiagnostico()).isEqualTo("Hiperplasia prostática");
        assertThat(cargoRepository.findByVisitaIdOrderByIdAsc(visita)).isEmpty();
    }

    @Test
    @DisplayName("Si la Fundación define la tarifa después de iniciar la visita, el cargo usa esa")
    void tarifaDefinidaDespuesDeIniciar() throws Exception {
        sinTarifa(7);
        Especialista especialista = nuevoEspecialista(7);
        long visita = idDe(iniciar(citaEl(especialista, HOY.atTime(11, 0)).getId(), especialista));
        diagnosticar(visita, especialista, "Infección urinaria", null).andExpect(status().isOk());

        tarifaDe(7, "180.00");
        finalizar(visita, especialista).andExpect(status().isOk());

        assertThat(cargoRepository.findByVisitaIdOrderByIdAsc(visita).get(0).getMontoOriginal())
                .isEqualByComparingTo("180.00");
        assertThat(visitaRepository.findById(visita).orElseThrow().getCostoConsulta())
                .isEqualByComparingTo("180.00");
    }

    // ---- YS-104: los antecedentes durante la visita (HU-08) ----

    @Test
    @DisplayName("Con la visita activa, el especialista ve las visitas anteriores del paciente con su diagnóstico, la más reciente primero")
    void veLosAntecedentes() throws Exception {
        Especialista primero = nuevoEspecialista(0);
        Especialista segundo = nuevoEspecialista(1);
        Especialista actual = nuevoEspecialista(0);
        Cita antigua = citaEl(primero, HOY.minusDays(2).atTime(8, 0), null);
        Long interno = antigua.getSolicitud().getInterno().getId();
        Cita reciente = citaEl(segundo, HOY.minusDays(1).atTime(8, 0), interno);
        Cita deHoy = citaEl(actual, HOY.atTime(8, 0), interno);
        // Otro paciente del mismo especialista: no es antecedente de este.
        Cita ajena = citaEl(primero, HOY.minusDays(1).atTime(9, 0), null);

        long visitaAntigua = idDe(iniciar(antigua.getId(), primero));
        cerrar(visitaAntigua, "Hipertensión arterial controlada", HOY.minusDays(2).atTime(8, 5));
        long visitaReciente = idDe(iniciar(reciente.getId(), segundo));
        cerrar(visitaReciente, "Deterioro cognitivo leve", HOY.minusDays(1).atTime(8, 5));
        idDe(iniciar(ajena.getId(), primero));
        long enCurso = idDe(iniciar(deHoy.getId(), actual));

        mvc.perform(get("/api/visitas/{id}/historial", enCurso)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(actual)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.internoId").value(interno))
                .andExpect(jsonPath("$.visitas.length()").value(2))
                .andExpect(jsonPath("$.visitas[0].id").value(visitaReciente))
                .andExpect(jsonPath("$.visitas[0].diagnostico").value("Deterioro cognitivo leve"))
                .andExpect(jsonPath("$.visitas[0].especialista.nombreCompleto")
                        .value(segundo.usuario().getNombreCompleto()))
                .andExpect(jsonPath("$.visitas[0].motivo").value("Control de seguimiento"))
                .andExpect(jsonPath("$.visitas[1].id").value(visitaAntigua))
                .andExpect(jsonPath("$.visitas[1].diagnostico").value("Hipertensión arterial controlada"));
    }

    @Test
    @DisplayName("Con la visita finalizada, los antecedentes ya no se pueden consultar: 403")
    void conLaVisitaFinalizadaNo() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Cita cita = citaEl(especialista, HOY.atTime(9, 0), null);
        long visita = idDe(iniciar(cita.getId(), especialista));
        cerrar(visita, "Sin hallazgos", HOY.atTime(9, 5));

        mvc.perform(get("/api/visitas/{id}/historial", visita)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value(containsString("solo durante la visita activa")));
    }

    @Test
    @DisplayName("Otro especialista no ve los antecedentes de una visita ajena: 403")
    void otroEspecialistaNoVeLosAntecedentes() throws Exception {
        Especialista especialista = nuevoEspecialista(0);
        Especialista otro = nuevoEspecialista(0);
        Cita cita = citaEl(especialista, HOY.atTime(9, 30), null);
        long visita = idDe(iniciar(cita.getId(), especialista));

        mvc.perform(get("/api/visitas/{id}/historial", visita)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(otro)))
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

    // ---- YS-67: el historial médico centralizado (HU-02) ----

    @Test
    @DisplayName("El Médico General ve todas las consultas del interno, la más reciente primero, con lo recetado y ordenado en cada una")
    void elHistorialCentralizado() throws Exception {
        Especialista primero = nuevoEspecialista(0);
        Especialista segundo = nuevoEspecialista(1);
        Cita antigua = citaEl(primero, HOY.minusDays(3).atTime(8, 0), null);
        Long interno = antigua.getSolicitud().getInterno().getId();
        Cita reciente = citaEl(segundo, HOY.minusDays(1).atTime(8, 0), interno);
        Cita deHoy = citaEl(primero, HOY.atTime(10, 0), interno);
        // Otro paciente: no es parte de este historial.
        Cita ajena = citaEl(segundo, HOY.atTime(9, 0), null);

        long visitaAntigua = idDe(iniciar(antigua.getId(), primero));
        recetar(visitaAntigua, primero, null, medicamento("Losartán", "50 mg", 30, "Cada 24 horas, vía oral"))
                .andExpect(status().isCreated());
        ordenar(visitaAntigua, primero, examen("Perfil lipídico", "En ayunas de 12 horas"))
                .andExpect(status().isCreated());
        cerrar(visitaAntigua, "Hipertensión arterial estadio 1", HOY.minusDays(3).atTime(8, 5));
        long visitaReciente = idDe(iniciar(reciente.getId(), segundo));
        cerrar(visitaReciente, "Deterioro cognitivo leve", HOY.minusDays(1).atTime(8, 5));
        idDe(iniciar(ajena.getId(), segundo));
        // La que sigue en curso también es historia: para el Médico General no
        // hay una consulta propia que excluir.
        long enCurso = idDe(iniciar(deHoy.getId(), primero));

        mvc.perform(get("/api/internos/{id}/historial", interno)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.MEDICO_GENERAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.internoId").value(interno))
                .andExpect(jsonPath("$.visitas.length()").value(3))
                .andExpect(jsonPath("$.visitas[0].id").value(enCurso))
                .andExpect(jsonPath("$.visitas[0].estado").value("ACTIVA"))
                .andExpect(jsonPath("$.visitas[1].id").value(visitaReciente))
                .andExpect(jsonPath("$.visitas[1].diagnostico").value("Deterioro cognitivo leve"))
                .andExpect(jsonPath("$.visitas[1].especialista.nombreCompleto")
                        .value(segundo.usuario().getNombreCompleto()))
                .andExpect(jsonPath("$.visitas[2].id").value(visitaAntigua))
                .andExpect(jsonPath("$.visitas[2].diagnostico").value("Hipertensión arterial estadio 1"))
                // La receta y el examen dicen de qué consulta salieron, para agruparlos.
                .andExpect(jsonPath("$.recetas.length()").value(1))
                .andExpect(jsonPath("$.recetas[0].visitaId").value(visitaAntigua))
                .andExpect(jsonPath("$.recetas[0].medicamentos[0].medicamento").value("Losartán"))
                .andExpect(jsonPath("$.examenes.length()").value(1))
                .andExpect(jsonPath("$.examenes[0].visitaId").value(visitaAntigua))
                .andExpect(jsonPath("$.examenes[0].examen.nombre").value("Perfil lipídico"))
                .andExpect(jsonPath("$.examenes[0].estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("Un interno sin consultas tiene el historial vacío; uno que no existe, 404")
    void historialVacioOInexistente() throws Exception {
        String token = tokenDe(RolNombre.MEDICO_GENERAL);
        long sinVisitas = crearInterno(token);

        mvc.perform(get("/api/internos/{id}/historial", sinVisitas)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.internoId").value(sinVisitas))
                .andExpect(jsonPath("$.visitas").isEmpty())
                .andExpect(jsonPath("$.recetas").isEmpty())
                .andExpect(jsonPath("$.examenes").isEmpty());

        mvc.perform(get("/api/internos/{id}/historial", 999_999)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    /** RNF-03 para el Administrador del Asilo; HU-08 para el especialista, que lo ve solo dentro de su visita. */
    @ParameterizedTest
    @EnumSource(value = RolNombre.class, names = "MEDICO_GENERAL", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("El historial centralizado lo consulta solo el Médico General")
    void historialSoloDelMedicoGeneral(RolNombre rol) throws Exception {
        mvc.perform(get("/api/internos/{id}/historial", 1)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(rol)))
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

    private long idDe(ResultActions respuesta) throws Exception {
        String cuerpo = respuesta.andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    /**
     * Finaliza una visita directo en la base, con su diagnóstico: el cierre por
     * la API llega con HU-09. También fija el inicio, para que el orden de los
     * antecedentes no dependa de cuándo corrió cada línea de la prueba.
     */
    private void cerrar(long visitaId, String diagnostico, LocalDateTime inicio) {
        VisitaMedica visita = visitaRepository.findById(visitaId).orElseThrow();
        visita.setFechaInicio(inicio);
        visita.setFechaCierre(inicio.plusMinutes(30));
        visita.setDiagnostico(diagnostico);
        visita.setEstado(EstadoVisita.FINALIZADA);
        visitaRepository.save(visita);
    }

    private ResultActions diagnosticar(long visitaId, Especialista especialista, String diagnostico,
            String observaciones) throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("diagnostico", diagnostico);
        cuerpo.put("observaciones", observaciones);
        return mvc.perform(put("/api/visitas/{id}/diagnostico", visitaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo)));
    }

    @SafeVarargs
    private ResultActions ordenar(long visitaId, Especialista especialista, Map<String, Object>... examenes)
            throws Exception {
        return mvc.perform(post("/api/visitas/{id}/ordenes-laboratorio", visitaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("examenes", List.of(examenes)))));
    }

    /** Un examen del catálogo, buscado por su nombre en la lista base. */
    private Map<String, Object> examen(String nombre, String indicaciones) {
        Long id = examenRepository.findByActivoTrue().stream()
                .filter(e -> e.getNombre().equals(nombre)).findFirst().orElseThrow().getId();
        Map<String, Object> examen = new LinkedHashMap<>();
        examen.put("examenId", id);
        examen.put("indicaciones", indicaciones);
        return examen;
    }

    private ResultActions iniciarOrden(Long ordenId) throws Exception {
        return mvc.perform(patch("/api/ordenes-laboratorio/{id}/iniciar", ordenId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.LABORATORIO)));
    }

    /**
     * Los ids de la bandeja del laboratorio para ese paciente, en los estados
     * dados (ninguno, todos). En una sola página grande: la base es
     * compartida y la prueba busca sus propias órdenes en la respuesta.
     */
    private List<Integer> idsEnBandeja(String paciente, String... estados) throws Exception {
        MockHttpServletRequestBuilder peticion = get("/api/ordenes-laboratorio")
                .param("paciente", paciente).param("size", "1000")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.LABORATORIO));
        if (estados.length > 0) {
            peticion.param("estado", estados);
        }
        String respuesta = mvc.perform(peticion)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(respuesta, "$.content[*].id");
    }

    private static String dpiDe(Cita cita) {
        return cita.getSolicitud().getInterno().getDpi();
    }

    /** YS-125. Lo mismo que {@link #idsEnBandeja}, en la bandeja de la farmacia. */
    private List<Integer> idsDeRecetas(String paciente, String... estados) throws Exception {
        MockHttpServletRequestBuilder peticion = get("/api/recetas")
                .param("paciente", paciente).param("size", "1000")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.FARMACIA));
        if (estados.length > 0) {
            peticion.param("estado", estados);
        }
        String respuesta = mvc.perform(peticion)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(respuesta, "$.content[*].id");
    }

    /**
     * YS-128. La farmacia despacha la receta entregando esos medicamentos; sin
     * ninguno, sin entregas. YS-131: cada uno con un costo cualquiera.
     */
    private ResultActions despachar(Long recetaId, Long... entregados) throws Exception {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Long id : entregados) {
            lista.add(entrega(id, "25.00"));
        }
        return despacharCon(recetaId, json.writeValueAsString(Map.of("entregados", lista)));
    }

    /** YS-131. Lo mismo, con el costo que registra la farmacia para cada uno (HU-17). */
    @SafeVarargs
    private ResultActions despacharEntregas(Long recetaId, Map<String, Object>... entregas) throws Exception {
        return despacharCon(recetaId, json.writeValueAsString(Map.of("entregados", List.of(entregas))));
    }

    private static Map<String, Object> entrega(Long id, String costo) {
        Map<String, Object> entrega = new LinkedHashMap<>();
        entrega.put("id", id);
        entrega.put("costo", costo);
        return entrega;
    }

    /**
     * YS-135. Una visita con los tres tipos de cargo: el examen con su costo
     * (HU-14), el medicamento entregado con el suyo (HU-17) y la consulta al
     * finalizar (HU-09). La especialidad ya tiene que tener tarifa.
     */
    private long conLosTresCargos(Cita cita, Especialista especialista, String nombreExamen, String costoExamen,
            String nombreMedicamento, String costoMedicamento) throws Exception {
        long visita = idDe(iniciar(cita.getId(), especialista));
        ordenar(visita, especialista, examen(nombreExamen, null)).andExpect(status().isCreated());
        Long orden = ordenRepository.findByVisitaIdOrderByIdAsc(visita).get(0).getId();
        cargarResultado(orden, "Dentro de lo normal", costoExamen, null).andExpect(status().isOk());
        recetar(visita, especialista, null, medicamento(nombreMedicamento, "25 mg", 30, "Cada noche"))
                .andExpect(status().isCreated());
        Receta receta = recetaRepository.findByVisitaId(visita).orElseThrow();
        despacharEntregas(receta.getId(), entrega(idsDe(receta)[0], costoMedicamento)).andExpect(status().isOk());
        diagnosticar(visita, especialista, "Hipertensión arterial", null).andExpect(status().isOk());
        finalizar(visita, especialista).andExpect(status().isOk());
        return visita;
    }

    /** YS-144. Las citas pendientes del paciente (nombre o DPI) en el rango; {@code null} lo omite. */
    private ResultActions citasPendientes(String paciente, LocalDate desde, LocalDate hasta) throws Exception {
        MockHttpServletRequestBuilder peticion = get("/api/servicios-pendientes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO));
        if (paciente != null) {
            peticion.param("paciente", paciente);
        }
        if (desde != null) {
            peticion.param("desde", desde.toString());
        }
        if (hasta != null) {
            peticion.param("hasta", hasta.toString());
        }
        return mvc.perform(peticion);
    }

    /** YS-141. Una visita finalizada con solo su consulta: un cargo con la tarifa de la especialidad. */
    private long consultaFinalizada(Especialista especialista, LocalDateTime momento) throws Exception {
        return consultaFinalizada(especialista, momento, null);
    }

    /** YS-144. Lo mismo, para un interno que ya existe; con {@code null}, uno nuevo. */
    private long consultaFinalizada(Especialista especialista, LocalDateTime momento, Long internoId)
            throws Exception {
        Cita cita = citaEl(especialista, momento, internoId);
        long visita = idDe(iniciar(cita.getId(), especialista));
        diagnosticar(visita, especialista, "Control", null).andExpect(status().isOk());
        finalizar(visita, especialista).andExpect(status().isOk());
        return visita;
    }

    /**
     * YS-141. Mueve los cargos de la visita a un mes propio de la prueba: la
     * base es compartida, y la deuda del mes en curso suma los cargos de todas.
     */
    private void cargosAlMes(long visitaId, YearMonth mes) {
        List<CargoServicio> cargos = cargoRepository.findByVisitaIdOrderByIdAsc(visitaId);
        cargos.forEach(cargo -> cargo.setFecha(mes.atDay(15)));
        cargoRepository.saveAll(cargos);
    }

    /** YS-141. La deuda de un mes y servicio dentro de la lista de cuentas por pagar. */
    private static String deuda(YearMonth mes, String tipoServicio) {
        return "$[?(@.periodo == '%s' && @.tipoServicio == '%s')]".formatted(mes, tipoServicio);
    }

    private ResultActions cuentasPorPagar() throws Exception {
        return mvc.perform(get("/api/cuentas-por-pagar")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO)));
    }

    /** YS-141. El Administrador del Asilo le paga a la fundación; {@code monto} va tal cual en el JSON. */
    private ResultActions pagarALaFundacion(YearMonth mes, String tipoServicio, String monto, LocalDate fecha,
            String formaPago) throws Exception {
        return mvc.perform(post("/api/cuentas-por-pagar/{periodo}/{tipo}/pagos", mes, tipoServicio)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"monto\": %s, \"fecha\": \"%s\", \"formaPago\": \"%s\"}".formatted(monto, fecha, formaPago)));
    }

    private ResultActions pagosDeLaFundacion(YearMonth mes, String tipoServicio) throws Exception {
        return mvc.perform(get("/api/cuentas-por-pagar/{periodo}/{tipo}/pagos", mes, tipoServicio)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO)));
    }

    private ResultActions anularPagoDeLaFundacion(long pagoId, String motivo) throws Exception {
        return mvc.perform(post("/api/pagos-fundacion/{id}/anulacion", pagoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("motivo", motivo))));
    }

    /** YS-178. Consolida el mes en curso y devuelve la cuenta del interno. */
    private long cuentaDelMes(long internoId) throws Exception {
        consolidar(YearMonth.now().toString()).andExpect(status().isOk());
        return cuentaRepository.findByInternoIdAndPeriodo(internoId, YearMonth.now().toString()).orElseThrow().getId();
    }

    /**
     * YS-178. Una cuenta de un mes pasado, guardada directo en la base como
     * estaba el último día de su mes: sin pagos y sin mora. Así no se consolida
     * un mes pasado para todos los internos de la base compartida.
     */
    private long cuentaAntesDeVencer(long internoId, YearMonth periodo, String total) {
        Interno interno = internoRepository.findById(internoId).orElseThrow();
        CuentaMensual cuenta = new CuentaMensual();
        cuenta.setInterno(interno);
        cuenta.setFamiliar(interno.getFamiliarResponsable());
        cuenta.setPeriodo(periodo.toString());
        cuenta.setCuotaBase(new BigDecimal(total));
        cuenta.setTotalServicios(BigDecimal.ZERO.setScale(2));
        cuenta.setTotalDescuentos(BigDecimal.ZERO.setScale(2));
        cuenta.setTotalAPagar(new BigDecimal(total));
        cuenta.actualizarSaldo(periodo.atEndOfMonth());
        cuenta.setFechaConsolidacion(periodo.atEndOfMonth().atTime(18, 0));
        return cuentaRepository.save(cuenta).getId();
    }

    /** YS-178. El Administrador del Asilo registra un pago; {@code monto} va tal cual en el JSON. */
    private ResultActions pagar(long cuentaId, String monto, LocalDate fecha, String formaPago) throws Exception {
        return pagarCon(cuentaId, "{\"monto\": %s, \"fecha\": \"%s\", \"formaPago\": \"%s\"}"
                .formatted(monto, fecha, formaPago));
    }

    private ResultActions pagarCon(long cuentaId, String cuerpo) throws Exception {
        return mvc.perform(post("/api/cuentas/{id}/pagos", cuentaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private ResultActions pagos(long cuentaId) throws Exception {
        return mvc.perform(get("/api/cuentas/{id}/pagos", cuentaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO)));
    }

    private ResultActions anular(long pagoId, String motivo) throws Exception {
        return mvc.perform(post("/api/pagos/{id}/anulacion", pagoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("motivo", motivo))));
    }

    /** YS-138. La fundación define el descuento de un servicio desde una fecha. */
    private ResultActions definirDescuento(String tipoServicio, String tipoDescuento, String valor, LocalDate desde)
            throws Exception {
        return mvc.perform(put("/api/politicas-descuento/{tipo}", tipoServicio)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_FUNDACION))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoDescuento\": \"%s\", \"valor\": %s, \"vigenteDesde\": \"%s\"}"
                        .formatted(tipoDescuento, valor, desde)));
    }

    /** YS-135. El Administrador del Asilo fija la cuota; {@code cuota} va tal cual en el JSON. */
    private ResultActions cambiarCuota(Long internoId, String cuota) throws Exception {
        return mvc.perform(put("/api/internos/{id}/cuota", internoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cuotaMensual\": %s}".formatted(cuota)));
    }

    private ResultActions consolidar(String periodo) throws Exception {
        return mvc.perform(post("/api/cuentas/consolidar").param("periodo", periodo)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO)));
    }

    private ResultActions estadoDeCuenta(Long internoId, LocalDate desde, LocalDate hasta) throws Exception {
        return mvc.perform(get("/api/internos/{id}/estado-cuenta", internoId)
                .param("desde", desde.toString()).param("hasta", hasta.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO)));
    }

    private ResultActions costosDe(Long recetaId) throws Exception {
        return mvc.perform(get("/api/recetas/{id}/costos", recetaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.FARMACIA)));
    }

    private List<CargoServicio> cargosDeFarmacia(long visitaId) {
        return cargoRepository.findByVisitaIdOrderByIdAsc(visitaId).stream()
                .filter(cargo -> cargo.getTipo() == TipoServicio.FARMACIA)
                .toList();
    }

    private ResultActions despacharCon(Long recetaId, String cuerpo) throws Exception {
        return mvc.perform(post("/api/recetas/{id}/despacho", recetaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.FARMACIA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    /** Los ids de los medicamentos de la receta, en el orden en que se recetaron. */
    private static Long[] idsDe(Receta receta) {
        return receta.getMedicamentos().stream().map(DetalleReceta::getId).toArray(Long[]::new);
    }

    /** Si cada medicamento de la receta de esa visita está entregado, en su orden. */
    private List<Boolean> entregados(long visitaId) {
        return recetaRepository.findByVisitaId(visitaId).orElseThrow().getMedicamentos().stream()
                .map(DetalleReceta::isEntregado).toList();
    }

    /** YS-119. El laboratorio carga el resultado con un costo cualquiera; {@code archivo} puede ser {@code null}. */
    private ResultActions cargarResultado(Long ordenId, String resultado, MockMultipartFile archivo)
            throws Exception {
        return cargarResultado(ordenId, resultado, "120.00", archivo);
    }

    /** YS-122. Lo mismo, con el costo que registra el laboratorio (HU-14). */
    private ResultActions cargarResultado(Long ordenId, String resultado, String costo, MockMultipartFile archivo)
            throws Exception {
        MockMultipartHttpServletRequestBuilder peticion = multipart("/api/ordenes-laboratorio/{id}/resultado", ordenId);
        if (archivo != null) {
            peticion.file(archivo);
        }
        return mvc.perform(peticion.param("resultado", resultado).param("costo", costo)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.LABORATORIO)));
    }

    private ResultActions costoDe(Long ordenId) throws Exception {
        return mvc.perform(get("/api/ordenes-laboratorio/{id}/costo", ordenId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.LABORATORIO)));
    }

    private List<CargoServicio> cargosDeLaboratorio(long visitaId) {
        return cargoRepository.findByVisitaIdOrderByIdAsc(visitaId).stream()
                .filter(cargo -> cargo.getTipo() == TipoServicio.LABORATORIO)
                .toList();
    }

    private static MockMultipartFile archivo(String nombre, String tipo, byte[] contenido) {
        return new MockMultipartFile("archivo", nombre, tipo, contenido);
    }

    private ResultActions descargar(Long ordenId, String token) throws Exception {
        return mvc.perform(get("/api/ordenes-laboratorio/{id}/archivo", ordenId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    @SafeVarargs
    private ResultActions recetar(long visitaId, Especialista especialista, String indicaciones,
            Map<String, Object>... medicamentos) throws Exception {
        return mvc.perform(post("/api/visitas/{id}/receta", visitaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpoReceta(indicaciones, medicamentos))));
    }

    @SafeVarargs
    private ResultActions corregir(long visitaId, Especialista especialista, String indicaciones,
            Map<String, Object>... medicamentos) throws Exception {
        return mvc.perform(put("/api/visitas/{id}/receta", visitaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpoReceta(indicaciones, medicamentos))));
    }

    private static Map<String, Object> cuerpoReceta(String indicaciones, Map<String, Object>[] medicamentos) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("indicacionesGenerales", indicaciones);
        cuerpo.put("medicamentos", List.of(medicamentos));
        return cuerpo;
    }

    /** Un medicamento de la receta; los null viajan como null, para probar los obligatorios. */
    private static Map<String, Object> medicamento(String nombre, String dosis, Integer cantidad, String forma) {
        Map<String, Object> medicamento = new LinkedHashMap<>();
        medicamento.put("medicamento", nombre);
        medicamento.put("dosis", dosis);
        medicamento.put("cantidad", cantidad);
        medicamento.put("formaAplicacion", forma);
        return medicamento;
    }

    private ResultActions finalizar(long visitaId, Especialista especialista) throws Exception {
        return mvc.perform(patch("/api/visitas/{id}/finalizar", visitaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(especialista)));
    }

    /** Fija la tarifa directo en la base: la API de tarifas ya tiene sus propias pruebas. */
    private void tarifaDe(int especialidad, String monto) {
        Especialidad encontrada = especialidadRepository.findById(idEspecialidad(especialidad)).orElseThrow();
        encontrada.setTarifaConsulta(new BigDecimal(monto));
        especialidadRepository.save(encontrada);
    }

    private void sinTarifa(int especialidad) {
        Especialidad encontrada = especialidadRepository.findById(idEspecialidad(especialidad)).orElseThrow();
        encontrada.setTarifaConsulta(null);
        especialidadRepository.save(encontrada);
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
        return citaEl(especialista, momento, null);
    }

    /** Lo mismo, para un interno que ya existe; con {@code null}, uno nuevo. */
    private Cita citaEl(Especialista especialista, LocalDateTime momento, Long internoId) throws Exception {
        Long solicitud = crearSolicitud(especialista.especialidad(), internoId);
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

    /** Una solicitud pendiente de la especialidad en esa posición; sin interno, para uno nuevo. */
    private Long crearSolicitud(int especialidad, Long internoExistente) throws Exception {
        String token = tokenDe(RolNombre.MEDICO_GENERAL);
        long internoId = internoExistente != null ? internoExistente : crearInterno(token);

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

    private long crearInterno(String token) throws Exception {
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
        String respuesta = mvc.perform(post("/api/internos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(interno))
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
