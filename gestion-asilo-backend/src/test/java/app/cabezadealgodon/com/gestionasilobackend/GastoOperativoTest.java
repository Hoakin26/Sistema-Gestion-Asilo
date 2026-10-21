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
 * YS-150. Los gastos operativos (HU-23, RF-16): que se registren con su tipo,
 * que el tipo Otro pida su descripción, que el histórico se filtre por tipo y
 * por fechas con sus totales, que uno mal registrado se anule con su motivo y
 * que solo el Administrador del Asilo entre.
 *
 * <p>
 * Cada prueba usa las fechas de un año propio y lejano: la base es compartida
 * y el histórico sin filtros trae los de todas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Gastos operativos (YS-150)")
class GastoOperativoTest extends PruebaConBaseDeDatos {

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
    @DisplayName("Un gasto válido se registra con su tipo y aparece en el histórico, con el total del período por tipo")
    void registraYApareceEnElHistorico() throws Exception {
        LocalDate dia = LocalDate.of(2021, 4, 10);
        registrar("LUZ", "  Recibo de marzo  ", "850.75", dia)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.tipoGasto").value("LUZ"))
                .andExpect(jsonPath("$.descripcion").value("Recibo de marzo"))
                .andExpect(jsonPath("$.monto").value(850.75))
                .andExpect(jsonPath("$.fecha").value("2021-04-10"))
                .andExpect(jsonPath("$.registradoPor").value("Persona de prueba"))
                .andExpect(jsonPath("$.anulado").value(false));
        // Sin descripción: con los tipos que no son Otro, es opcional.
        registrar("AGUA", null, "320", dia.plusDays(1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descripcion").isEmpty());
        registrar("OTRO", "Fumigación del edificio", "1200", dia.plusDays(2)).andExpect(status().isCreated());
        registrar("LUZ", "Recibo de abril", "900", dia.plusDays(3)).andExpect(status().isCreated());

        historico(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31), new String[0])
                .andExpect(status().isOk())
                // Del más reciente al más antiguo.
                .andExpect(jsonPath("$.gastos[*].tipoGasto").value(contains("LUZ", "OTRO", "AGUA", "LUZ")))
                .andExpect(jsonPath("$.total").value(3270.75))
                .andExpect(jsonPath("$.porTipo.length()").value(8))
                .andExpect(jsonPath("$.porTipo[?(@.tipoGasto == 'LUZ')].cantidad").value(2))
                .andExpect(jsonPath("$.porTipo[?(@.tipoGasto == 'LUZ')].total").value(1750.75))
                .andExpect(jsonPath("$.porTipo[?(@.tipoGasto == 'OTRO')].total").value(1200.00))
                .andExpect(jsonPath("$.porTipo[?(@.tipoGasto == 'GAS')].cantidad").value(0));
    }

    @Test
    @DisplayName("El histórico se filtra por tipo, uno o varios, y por rango de fechas con los dos días incluidos")
    void filtraElHistorico() throws Exception {
        registrar("AGUA", null, "100", LocalDate.of(2022, 8, 1)).andExpect(status().isCreated());
        registrar("GAS", null, "200", LocalDate.of(2022, 8, 31)).andExpect(status().isCreated());
        registrar("LIMPIEZA", "Detergente y cloro", "300", LocalDate.of(2022, 9, 1)).andExpect(status().isCreated());

        historico(LocalDate.of(2022, 8, 1), LocalDate.of(2022, 8, 31), new String[0])
                .andExpect(jsonPath("$.gastos[*].tipoGasto").value(contains("GAS", "AGUA")))
                .andExpect(jsonPath("$.total").value(300.00));
        historico(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31), "GAS")
                .andExpect(jsonPath("$.gastos[*].tipoGasto").value(contains("GAS")))
                .andExpect(jsonPath("$.total").value(200.00));
        historico(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31), "LIMPIEZA", "AGUA")
                .andExpect(jsonPath("$.gastos[*].tipoGasto").value(contains("LIMPIEZA", "AGUA")))
                .andExpect(jsonPath("$.total").value(400.00));

        historico(LocalDate.of(2022, 9, 1), LocalDate.of(2022, 8, 1), new String[0])
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.hasta").value("La fecha final no puede ser anterior a la inicial"));
        historico(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31), "INTERNET").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un monto de cero o negativo, una fecha futura, un Otro sin descripción o un dato faltante se rechazan sin registrar nada")
    void gastoInvalido() throws Exception {
        LocalDate dia = LocalDate.of(2023, 6, 1);
        registrar("LUZ", null, "0", dia)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").value("El monto tiene que ser mayor que cero"));
        registrar("LUZ", null, "-10", dia)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").value("El monto tiene que ser mayor que cero"));
        registrar("LUZ", null, "10.555", dia)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").value("El monto admite hasta dos decimales"));
        registrar("LUZ", null, "100", LocalDate.now().plusDays(1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fecha").value("La fecha del gasto no puede estar en el futuro"));
        registrar("OTRO", "   ", "100", dia)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.descripcion").value("Con el tipo Otro, describa en qué se gastó"));
        registrar("OTRO", null, "100", dia)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.descripcion").value("Con el tipo Otro, describa en qué se gastó"));
        registrar("INTERNET", null, "100", dia).andExpect(status().isBadRequest());
        registrarCon("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tipoGasto").value("Elija el tipo de gasto"))
                .andExpect(jsonPath("$.errores.monto").value("Indique el monto del gasto"))
                .andExpect(jsonPath("$.errores.fecha").value("Indique la fecha del gasto"));
        mvc.perform(get("/api/gastos-operativos")).andExpect(status().isUnauthorized());

        historico(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31), new String[0])
                .andExpect(jsonPath("$.gastos").isEmpty())
                .andExpect(jsonPath("$.total").value(0.00));
    }

    @Test
    @DisplayName("Anular un gasto lo deja en el histórico con el motivo y fuera de los totales; no se anula dos veces")
    void anularUnGasto() throws Exception {
        LocalDate dia = LocalDate.of(2024, 11, 5);
        registrar("ALIMENTACION", "Compra de la semana", "1500", dia).andExpect(status().isCreated());
        String respuesta = registrar("ALIMENTACION", "Compra de la semana", "15000", dia)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long gasto = ((Number) JsonPath.read(respuesta, "$.id")).longValue();

        anular(gasto, "  ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivo").value("Escriba el motivo de la anulación"));
        anular(gasto, "  Se registró dos veces, con un cero de más  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anulado").value(true))
                .andExpect(jsonPath("$.motivoAnulacion").value("Se registró dos veces, con un cero de más"))
                .andExpect(jsonPath("$.anuladoPor").value("Persona de prueba"))
                .andExpect(jsonPath("$.fechaAnulacion").isNotEmpty());
        anular(gasto, "Otra vez")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Este gasto ya está anulado"));
        anular(999_999L, "No existe").andExpect(status().isNotFound());

        historico(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), new String[0])
                .andExpect(jsonPath("$.gastos.length()").value(2))
                .andExpect(jsonPath("$.total").value(1500.00))
                .andExpect(jsonPath("$.porTipo[?(@.tipoGasto == 'ALIMENTACION')].cantidad").value(1));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = "ADMINISTRADOR_ASILO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Los otros seis roles no registran, ni consultan, ni anulan gastos: 403")
    void soloElAdministradorDelAsiloRegistraGastos(RolNombre rol) throws Exception {
        String token = "Bearer " + tokenDe(rol);
        // Con cuerpos válidos: la validación corre antes que la seguridad del método.
        mvc.perform(post("/api/gastos-operativos")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("LUZ", null, "100", LocalDate.of(2025, 1, 1))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/gastos-operativos").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/gastos-operativos/{id}/anulacion", 999_999)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\": \"Error de digitación\"}"))
                .andExpect(status().isForbidden());
    }

    /** {@code monto} va tal cual en el JSON; {@code descripcion} puede ir {@code null}. */
    private ResultActions registrar(String tipo, String descripcion, String monto, LocalDate fecha) throws Exception {
        return registrarCon(cuerpo(tipo, descripcion, monto, fecha));
    }

    private ResultActions registrarCon(String cuerpo) throws Exception {
        return mvc.perform(post("/api/gastos-operativos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private String cuerpo(String tipo, String descripcion, String monto, LocalDate fecha) {
        return "{\"tipoGasto\": \"%s\", \"descripcion\": %s, \"monto\": %s, \"fecha\": \"%s\"}"
                .formatted(tipo, json.writeValueAsString(descripcion), monto, fecha);
    }

    private ResultActions historico(LocalDate desde, LocalDate hasta, String... tipos) throws Exception {
        MockHttpServletRequestBuilder peticion = get("/api/gastos-operativos")
                .param("desde", desde.toString())
                .param("hasta", hasta.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO));
        for (String tipo : tipos) {
            peticion.param("tipo", tipo);
        }
        return mvc.perform(peticion);
    }

    private ResultActions anular(long id, String motivo) throws Exception {
        return mvc.perform(post("/api/gastos-operativos/{id}/anulacion", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_ASILO))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("motivo", motivo))));
    }

    private String tokenDe(RolNombre rol) throws Exception {
        String correo = rol.name().toLowerCase(Locale.ROOT) + ".gastos@asilo.gt";
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
