package app.cabezadealgodon.com.gestionasilobackend;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.RolRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * YS-147. Las donaciones (HU-22, RF-15): que se registren con su tipo de
 * donante, que el histórico se filtre por tipo y por fechas con sus totales,
 * que una mal registrada se anule con su motivo y que solo el Administrador del
 * Asilo entre.
 *
 * <p>
 * Cada prueba usa las fechas de un año propio y lejano: la base es compartida
 * y el histórico sin filtros trae las de todas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Donaciones (YS-147)")
class DonacionTest extends PruebaConBaseDeDatos {

    private static final String PASSWORD = "Prueba1234";

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

    @Test
    @DisplayName("Una donación válida se registra con su tipo de donante y aparece en el histórico, con el total del período por tipo")
    void registraYApareceEnElHistorico() throws Exception {
        LocalDate dia = LocalDate.of(2011, 3, 15);
        registrar("15000.50", dia, "  Fundación Esperanza Internacional  ", "EMPRESA_INTERNACIONAL")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.monto").value(15000.50))
                .andExpect(jsonPath("$.fecha").value("2011-03-15"))
                .andExpect(jsonPath("$.donante").value("Fundación Esperanza Internacional"))
                .andExpect(jsonPath("$.tipoDonante").value("EMPRESA_INTERNACIONAL"))
                .andExpect(jsonPath("$.registradaPor").value("Persona de prueba"))
                .andExpect(jsonPath("$.anulada").value(false));
        registrar("2500", dia.plusDays(1), "Municipalidad de Mazatenango", "GOBIERNO").andExpect(status().isCreated());
        registrar("300", dia.plusDays(2), "María López", "PARTICULAR").andExpect(status().isCreated());
        registrar("200", dia.plusDays(3), "Juan Pérez", "PARTICULAR").andExpect(status().isCreated());

        historico(LocalDate.of(2011, 1, 1), LocalDate.of(2011, 12, 31), new String[0])
                .andExpect(status().isOk())
                // De la más reciente a la más antigua.
                .andExpect(jsonPath("$.donaciones.content[*].donante").value(contains("Juan Pérez", "María López",
                        "Municipalidad de Mazatenango", "Fundación Esperanza Internacional")))
                .andExpect(jsonPath("$.total").value(18000.50))
                .andExpect(jsonPath("$.porTipo[*].tipoDonante").value(contains("EMPRESA_INTERNACIONAL",
                        "EMPRESA_NACIONAL", "GOBIERNO", "PARTICULAR")))
                .andExpect(jsonPath("$.porTipo[?(@.tipoDonante == 'EMPRESA_INTERNACIONAL')].total").value(15000.50))
                .andExpect(jsonPath("$.porTipo[?(@.tipoDonante == 'EMPRESA_NACIONAL')].cantidad").value(0))
                .andExpect(jsonPath("$.porTipo[?(@.tipoDonante == 'PARTICULAR')].cantidad").value(2))
                .andExpect(jsonPath("$.porTipo[?(@.tipoDonante == 'PARTICULAR')].total").value(500.00));
    }

    @Test
    @DisplayName("El histórico se filtra por tipo de donante, uno o varios, y por rango de fechas con los dos días incluidos")
    void filtraElHistorico() throws Exception {
        registrar("1000", LocalDate.of(2012, 5, 1), "Empresa del primero", "EMPRESA_NACIONAL").andExpect(status().isCreated());
        registrar("2000", LocalDate.of(2012, 5, 31), "Gobierno del último", "GOBIERNO").andExpect(status().isCreated());
        registrar("3000", LocalDate.of(2012, 6, 1), "Persona de junio", "PARTICULAR").andExpect(status().isCreated());

        historico(LocalDate.of(2012, 5, 1), LocalDate.of(2012, 5, 31), new String[0])
                .andExpect(jsonPath("$.donaciones.content[*].donante").value(contains("Gobierno del último",
                        "Empresa del primero")))
                .andExpect(jsonPath("$.total").value(3000.00));
        historico(LocalDate.of(2012, 1, 1), LocalDate.of(2012, 12, 31), "GOBIERNO")
                .andExpect(jsonPath("$.donaciones.content[*].donante").value(contains("Gobierno del último")))
                .andExpect(jsonPath("$.total").value(2000.00));
        historico(LocalDate.of(2012, 1, 1), LocalDate.of(2012, 12, 31), "PARTICULAR", "EMPRESA_NACIONAL")
                .andExpect(jsonPath("$.donaciones.content[*].donante").value(contains("Persona de junio",
                        "Empresa del primero")))
                .andExpect(jsonPath("$.total").value(4000.00));

        historico(LocalDate.of(2012, 6, 1), LocalDate.of(2012, 5, 1), new String[0])
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.hasta").value("La fecha final no puede ser anterior a la inicial"));
        historico(LocalDate.of(2012, 1, 1), LocalDate.of(2012, 12, 31), "EMPRESA").andExpect(status().isBadRequest());
    }

    /**
     * OBS-01. Con páginas de dos para no registrar veinte: lo que se prueba es
     * que la página trae solo su parte y los totales, todo el filtro.
     */
    @Test
    @DisplayName("El histórico llega por páginas de 20, la más reciente primero, y los totales suman todo el filtro")
    void historicoPorPaginas() throws Exception {
        registrar("100", LocalDate.of(2016, 2, 1), "Primera de 2016", "PARTICULAR").andExpect(status().isCreated());
        registrar("200", LocalDate.of(2016, 3, 1), "Segunda de 2016", "GOBIERNO").andExpect(status().isCreated());
        registrar("300", LocalDate.of(2016, 4, 1), "Tercera de 2016", "PARTICULAR").andExpect(status().isCreated());
        String respuesta = registrar("5000", LocalDate.of(2016, 5, 1), "Anulada de 2016", "GOBIERNO")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        anular(((Number) JsonPath.read(respuesta, "$.id")).longValue(), "Se registró dos veces")
                .andExpect(status().isOk());

        historico(LocalDate.of(2016, 1, 1), LocalDate.of(2016, 12, 31), new String[0])
                .andExpect(jsonPath("$.donaciones.size").value(20))
                .andExpect(jsonPath("$.donaciones.totalElements").value(4));
        pagina(0, 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donaciones.content[*].donante").value(contains("Anulada de 2016",
                        "Tercera de 2016")))
                .andExpect(jsonPath("$.donaciones.totalElements").value(4))
                .andExpect(jsonPath("$.donaciones.totalPages").value(2))
                // Las anuladas se ven en la lista, pero no suman.
                .andExpect(jsonPath("$.total").value(600.00))
                .andExpect(jsonPath("$.porTipo[?(@.tipoDonante == 'PARTICULAR')].cantidad").value(2))
                .andExpect(jsonPath("$.porTipo[?(@.tipoDonante == 'GOBIERNO')].total").value(200.00));
        pagina(1, 2)
                .andExpect(jsonPath("$.donaciones.content[*].donante").value(contains("Segunda de 2016",
                        "Primera de 2016")))
                .andExpect(jsonPath("$.total").value(600.00));

        // El orden es fijo: pedir otro es 400 y no un 500.
        mvc.perform(get("/api/donaciones").param("sort", "monto")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un monto de cero, negativo o con tres decimales, una fecha futura o un dato faltante se rechazan sin registrar nada")
    void donacionInvalida() throws Exception {
        LocalDate dia = LocalDate.of(2013, 7, 1);
        registrar("0", dia, "Donante", "PARTICULAR")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").value("El monto tiene que ser mayor que cero"));
        registrar("-50", dia, "Donante", "PARTICULAR")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").value("El monto tiene que ser mayor que cero"));
        registrar("10.555", dia, "Donante", "PARTICULAR")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").value("El monto admite hasta dos decimales"));
        registrar("100", LocalDate.now().plusDays(1), "Donante", "PARTICULAR")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fecha").value("La fecha de la donación no puede estar en el futuro"));
        registrar("100", dia, "   ", "PARTICULAR")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.donante").value("Escriba el nombre del donante"));
        registrar("100", dia, "Donante", "EMPRESA").andExpect(status().isBadRequest());
        registrarCon("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").value("Indique el monto de la donación"))
                .andExpect(jsonPath("$.errores.fecha").value("Indique la fecha de la donación"))
                .andExpect(jsonPath("$.errores.donante").value("Escriba el nombre del donante"))
                .andExpect(jsonPath("$.errores.tipoDonante").value("Elija el tipo de donante"));
        mvc.perform(get("/api/donaciones")).andExpect(status().isUnauthorized());

        historico(LocalDate.of(2013, 1, 1), LocalDate.of(2013, 12, 31), new String[0])
                .andExpect(jsonPath("$.donaciones.content").isEmpty())
                .andExpect(jsonPath("$.total").value(0.00));
    }

    @Test
    @DisplayName("Anular una donación la deja en el histórico con el motivo y fuera de los totales; no se anula dos veces")
    void anularUnaDonacion() throws Exception {
        LocalDate dia = LocalDate.of(2014, 2, 10);
        registrar("800", dia, "Donante correcto", "PARTICULAR").andExpect(status().isCreated());
        String respuesta = registrar("8000", dia, "Donante con un cero de más", "PARTICULAR")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long donacion = ((Number) JsonPath.read(respuesta, "$.id")).longValue();

        anular(donacion, "  ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivo").value("Escriba el motivo de la anulación"));
        anular(donacion, "  Se registró Q8000 en lugar de Q800  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anulada").value(true))
                .andExpect(jsonPath("$.motivoAnulacion").value("Se registró Q8000 en lugar de Q800"))
                .andExpect(jsonPath("$.anuladaPor").value("Persona de prueba"))
                .andExpect(jsonPath("$.fechaAnulacion").isNotEmpty());
        anular(donacion, "Otra vez")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Esta donación ya está anulada"));
        anular(999_999L, "No existe").andExpect(status().isNotFound());

        historico(LocalDate.of(2014, 1, 1), LocalDate.of(2014, 12, 31), new String[0])
                .andExpect(jsonPath("$.donaciones.content.length()").value(2))
                .andExpect(jsonPath("$.total").value(800.00))
                .andExpect(jsonPath("$.porTipo[?(@.tipoDonante == 'PARTICULAR')].cantidad").value(1));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = "ADMINISTRADOR_ASILO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Los otros seis roles no registran, ni consultan, ni anulan donaciones: 403")
    void soloElAdministradorDelAsiloRegistraDonaciones(RolNombre rol) throws Exception {
        String token = "Bearer " + tokenDe(rol);
        // Con cuerpos válidos: la validación corre antes que la seguridad del método.
        mvc.perform(post("/api/donaciones")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("100", LocalDate.of(2015, 1, 1), "Donante", "PARTICULAR")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/donaciones").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/donaciones/{id}/anulacion", 999_999)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\": \"Error de digitación\"}"))
                .andExpect(status().isForbidden());
    }

    /** {@code monto} va tal cual en el JSON. */
    private ResultActions registrar(String monto, LocalDate fecha, String donante, String tipo) throws Exception {
        return registrarCon(cuerpo(monto, fecha, donante, tipo));
    }

    private ResultActions registrarCon(String cuerpo) throws Exception {
        return mvc.perform(post("/api/donaciones")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private String cuerpo(String monto, LocalDate fecha, String donante, String tipo) {
        return "{\"monto\": %s, \"fecha\": \"%s\", \"donante\": %s, \"tipoDonante\": \"%s\"}"
                .formatted(monto, fecha, json.writeValueAsString(donante), tipo);
    }

    private ResultActions historico(LocalDate desde, LocalDate hasta, String... tipos) throws Exception {
        MockHttpServletRequestBuilder peticion = get("/api/donaciones")
                .param("desde", desde.toString())
                .param("hasta", hasta.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO));
        for (String tipo : tipos) {
            peticion.param("tipo", tipo);
        }
        return mvc.perform(peticion);
    }

    /** OBS-01. Una página del histórico de 2016, con su tamaño. */
    private ResultActions pagina(int numero, int tamano) throws Exception {
        return mvc.perform(get("/api/donaciones")
                .param("desde", "2016-01-01")
                .param("hasta", "2016-12-31")
                .param("page", Integer.toString(numero))
                .param("size", Integer.toString(tamano))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO)));
    }

    private ResultActions anular(long id, String motivo) throws Exception {
        return mvc.perform(post("/api/donaciones/{id}/anulacion", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("motivo", motivo))));
    }

    private String tokenDe(RolNombre rol) throws Exception {
        String correo = rol.name().toLowerCase(Locale.ROOT) + ".donaciones@asilo.gt";
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
