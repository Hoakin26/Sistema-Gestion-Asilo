package app.cabezadealgodon.com.gestionasilobackend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
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
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.RolRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;

/**
 * YS-62. El recorrido completo de HU-01 tal como lo hace la pantalla:
 * registrar, completar la ficha, editar y consultar.
 *
 * <p>
 * Las pruebas de YS-58 y YS-59 cubren cada endpoint y cada regla por separado.
 * Estas cubren lo que queda entre ellos: que editar el expediente no toque la
 * ficha, que lo guardado se lea igual desde otro rol, que ningún rol distinto
 * del Médico General pueda escribir por ninguna de las tres vías, y que la
 * búsqueda encuentre a las personas como el personal las escribe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("HU-01: registrar, editar y consultar un interno (YS-62)")
class InternoRecorridoTest extends PruebaConBaseDeDatos {

    private static final String PASSWORD = "Prueba1234";

    /*
     * Un DPI distinto por alta. La base es una sola para toda la ejecución
     * (PruebaConBaseDeDatos) y el DPI es único: un valor fijo chocaría con el
     * de otra prueba o con el de esta misma ejecutada dos veces.
     */
    private static final AtomicLong SIGUIENTE_DPI = new AtomicLong(6_100_000_000_000L);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("El alta deja una ficha médica vacía, lista para completarse desde el perfil")
    void elAltaDejaLaFichaVacia() throws Exception {
        Long id = registrar("Tomás Ixcoy Batz", nuevoDpi());

        mvc.perform(get("/api/internos/{id}/ficha-medica", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.MEDICO_GENERAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.internoId").value(id))
                .andExpect(jsonPath("$.psicopatologias").isEmpty())
                .andExpect(jsonPath("$.medicamentosHabituales").isEmpty());
    }

    @Test
    @DisplayName("El Médico General edita los datos y el familiar, y enfermería lee lo guardado")
    void loEditadoSeLeeDesdeOtroRol() throws Exception {
        String dpi = nuevoDpi();
        Long id = registrar("Julia Coyoy Tzul", dpi);

        actualizar(id, cuerpo("Julia Coyoy Tzul de Pérez", dpi, "Nieta", "4444 9876"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/internos/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ENFERMERIA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Julia Coyoy Tzul de Pérez"))
                .andExpect(jsonPath("$.familiarResponsable.parentesco").value("Nieta"))
                .andExpect(jsonPath("$.familiarResponsable.telefono").value("4444 9876"));
    }

    /**
     * La edición y la ficha van por endpoints distintos a propósito (hallazgo 7
     * de la trazabilidad). Si editar el expediente borrara la ficha, el
     * Médico General perdería los datos clínicos cada vez que corrige un
     * teléfono, y no lo notaría hasta que alguien los busque.
     */
    @Test
    @DisplayName("Editar el expediente no toca la ficha médica")
    void editarNoTocaLaFicha() throws Exception {
        String dpi = nuevoDpi();
        Long id = registrar("Marta Chen Xol", dpi);
        String token = tokenDe(RolNombre.MEDICO_GENERAL);

        mvc.perform(put("/api/internos/{id}/ficha-medica", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"alergias": "Penicilina",
                         "medicamentosHabituales": [
                           {"nombre": "Donepezilo", "dosis": "10 mg", "frecuencia": "Cada 24 horas"}]}
                        """))
                .andExpect(status().isOk());

        actualizar(id, cuerpo("Marta Chen Xol", dpi, "Hija", "5555 0001")).andExpect(status().isOk());

        mvc.perform(get("/api/internos/{id}/ficha-medica", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alergias").value("Penicilina"))
                .andExpect(jsonPath("$.medicamentosHabituales[0].nombre").value("Donepezilo"));
    }

    @Test
    @DisplayName("Editar con el DPI de otro interno se rechaza con 409; conservar el propio no")
    void elDpiDeOtroSeRechazaAlEditar() throws Exception {
        String dpiPrimero = nuevoDpi();
        registrar("Pedro Sic Ajú", dpiPrimero);
        String dpiSegundo = nuevoDpi();
        Long segundo = registrar("Pablo Sic Ajú", dpiSegundo);

        actualizar(segundo, cuerpo("Pablo Sic Ajú", dpiPrimero, "Hijo", "5555 0002"))
                .andExpect(status().isConflict());

        actualizar(segundo, cuerpo("Pablo Sic Ajú", dpiSegundo, "Hijo", "5555 0002"))
                .andExpect(status().isOk());
    }

    /**
     * RNF-02. Las tres vías de escritura del expediente, con cada rol que no
     * es el Médico General. Las pruebas de YS-58 solo probaban el alta y solo
     * con enfermería: un endpoint al que se le olvidara la anotación pasaría
     * sin que nada lo señalara.
     */
    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = "MEDICO_GENERAL", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Ningún rol distinto del Médico General registra, edita ni escribe la ficha")
    void soloElMedicoGeneralEscribe(RolNombre rol) throws Exception {
        String dpi = nuevoDpi();
        Long id = registrar("Ana Tum Coc", dpi);
        String token = tokenDe(rol);

        mvc.perform(post("/api/internos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("Otra Persona", nuevoDpi(), "Hijo", "5555 0003")))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/internos/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("Ana Tum Coc", dpi, "Hijo", "5555 0003")))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/internos/{id}/ficha-medica", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"alergias\": \"Ninguna\"}"))
                .andExpect(status().isForbidden());
    }

    /**
     * En la tableta casi nadie escribe tildes: buscar "nunez" tiene que
     * encontrar a "Núñez". La lista de usuarios ya se comportaba así; la de
     * internos distinguía tildes y dejaba a la persona sin resultados.
     */
    @Test
    @DisplayName("La búsqueda ignora tildes y mayúsculas")
    void laBusquedaIgnoraTildes() throws Exception {
        String dpi = nuevoDpi();
        registrar("Ángela Núñez Ibáñez", dpi);

        buscar("angela nunez").andExpect(jsonPath("$.content[?(@.dpi == '%s')]".formatted(dpi)).isNotEmpty());
        buscar("ÍBÁÑEZ").andExpect(jsonPath("$.content[?(@.dpi == '%s')]".formatted(dpi)).isNotEmpty());
    }

    /** La pantalla muestra el DPI como "1234 56789 0101", y así lo van a copiar. */
    @Test
    @DisplayName("La búsqueda por DPI acepta los espacios con que se muestra")
    void laBusquedaPorDpiAceptaEspacios() throws Exception {
        String dpi = nuevoDpi();
        registrar("Lucía Ajpop Can", dpi);

        String conEspacios = dpi.substring(0, 4) + " " + dpi.substring(4, 9) + " " + dpi.substring(9);
        buscar(conEspacios).andExpect(jsonPath("$.content[?(@.dpi == '%s')]".formatted(dpi)).isNotEmpty());
    }

    /** Un comodín escrito por el usuario cuenta como texto, no como "cualquier cosa". */
    @Test
    @DisplayName("Un % en la búsqueda no trae a todos los internos")
    void elComodinNoTraeATodos() throws Exception {
        registrar("Elena Pop Caal", nuevoDpi());

        buscar("%").andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @DisplayName("Un interno que no existe responde 404")
    void unInternoQueNoExiste() throws Exception {
        mvc.perform(get("/api/internos/{id}", 999_999)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.MEDICO_GENERAL)))
                .andExpect(status().isNotFound());
    }

    // ---- apoyo ----

    private static String nuevoDpi() {
        return String.valueOf(SIGUIENTE_DPI.getAndIncrement());
    }

    private static String cuerpo(String nombre, String dpi, String parentesco, String telefono) {
        return """
                {"nombreCompleto": "%s",
                 "dpi": "%s",
                 "fechaNacimiento": "1942-05-20",
                 "fechaIngreso": "2024-02-01",
                 "familiarResponsable": {
                   "nombreCompleto": "Familiar de prueba",
                   "parentesco": "%s",
                   "telefono": "%s"}}
                """.formatted(nombre, dpi, parentesco, telefono);
    }

    private Long registrar(String nombre, String dpi) throws Exception {
        String respuesta = mvc.perform(post("/api/internos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.MEDICO_GENERAL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(nombre, dpi, "Hijo", "5555 0000")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private ResultActions actualizar(Long id, String cuerpo) throws Exception {
        return mvc.perform(put("/api/internos/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.MEDICO_GENERAL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private ResultActions buscar(String termino) throws Exception {
        return mvc.perform(get("/api/internos").param("busqueda", termino)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ENFERMERIA)))
                .andExpect(status().isOk());
    }

    /** Un usuario por rol, rehecho en cada llamada como en AutorizacionTest. */
    private String tokenDe(RolNombre rol) throws Exception {
        String correo = rol.name().toLowerCase(Locale.ROOT) + ".recorrido@asilo.gt";

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
