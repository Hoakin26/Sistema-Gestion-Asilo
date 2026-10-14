package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import app.cabezadealgodon.com.gestionasilobackend.models.CargoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.Cita;
import app.cabezadealgodon.com.gestionasilobackend.models.Especialidad;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoReceta;
import app.cabezadealgodon.com.gestionasilobackend.models.OrdenLaboratorio;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoSolicitud;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoVisita;
import app.cabezadealgodon.com.gestionasilobackend.models.Receta;
import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.SolicitudReferencia;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.models.VisitaMedica;
import app.cabezadealgodon.com.gestionasilobackend.repository.CargoServicioRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.CitaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.EspecialidadRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.ExamenLaboratorioRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.OrdenLaboratorioRepository;
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
 * (YS-113), los exámenes de laboratorio.
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
        recetar(visita, especialista, null, medicamento("Losartán", "50 mg", 30, "Cada 24 horas"))
                .andExpect(status().isCreated());
        // El despacho llega con HU-16: aquí se marca a mano.
        Receta receta = recetaRepository.findByVisitaId(visita).orElseThrow();
        receta.setEstado(EstadoReceta.PARCIALMENTE_ENTREGADA);
        recetaRepository.save(receta);

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
