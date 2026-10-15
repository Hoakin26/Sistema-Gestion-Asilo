package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jayway.jsonpath.JsonPath;

import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.RolRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;
import app.cabezadealgodon.com.gestionasilobackend.security.SoloMedicoGeneral;
import jakarta.servlet.http.HttpServletResponse;

/**
 * YS-172. Un error inesperado del servidor no cierra la sesión: sale como 500,
 * con el formato de error de la API y un mensaje genérico, y el token sigue
 * valiendo. El 401 queda solo para la sesión que de verdad falta.
 *
 * <p>
 * Se prueba contra un servidor de verdad, en un puerto al azar, y no con
 * MockMvc. El defecto vivía en el reenvío interno a {@code /error} que hace el
 * contenedor después de una falla, y MockMvc no hace ese reenvío: con él, estas
 * mismas peticiones respondían bien y la prueba no habría visto nada.
 *
 * <p>
 * Las fallas salen de un controlador que solo existe en esta prueba.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ErrorInesperadoTest.ControladorConFallas.class)
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("Un error inesperado no cierra la sesión (YS-172)")
class ErrorInesperadoTest extends PruebaConBaseDeDatos {

    private static final String PASSWORD = "Prueba1234";
    private static final String CORREO = "medico.general.errores@asilo.gt";
    /** Lo que nunca debe llegar al usuario: el detalle interno de la falla. */
    private static final String DETALLE_INTERNO = "Detalle interno: tabla interno, columna dpi";

    private final HttpClient cliente = HttpClient.newHttpClient();

    @LocalServerPort
    private int puerto;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Una excepción no prevista responde 500 con un mensaje genérico, la traza queda en el log y la sesión sigue valiendo")
    void excepcionNoPrevista(CapturedOutput salida) throws Exception {
        String token = token();

        HttpResponse<String> respuesta = pedir("GET", "/api/pruebas-de-error/excepcion", token);

        assertThat(respuesta.statusCode()).isEqualTo(500);
        assertThat(mensajeDe(respuesta)).startsWith("Ocurrió un error inesperado");
        assertThat(respuesta.body()).doesNotContain(DETALLE_INTERNO);
        // Quien tenga que corregirlo encuentra la traza en el log del servidor.
        assertThat(salida.getAll()).contains(DETALLE_INTERNO);
        // Y la misma sesión sigue funcionando en la petición siguiente.
        assertThat(pedir("GET", "/api/internos", token).statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("Un error de Java, como el de una clase compilada con errores, también sale como 500 y no como 401")
    void errorDeJava() throws Exception {
        HttpResponse<String> respuesta = pedir("GET", "/api/pruebas-de-error/error-de-java", token());

        assertThat(respuesta.statusCode()).isEqualTo(500);
        assertThat(mensajeDe(respuesta)).startsWith("Ocurrió un error inesperado");
        assertThat(respuesta.body()).doesNotContain(DETALLE_INTERNO);
    }

    @Test
    @DisplayName("Lo que el contenedor resuelve con su reenvío interno a /error conserva su estado y el formato de la API")
    void reenvioInterno() throws Exception {
        HttpResponse<String> respuesta = pedir("GET", "/api/pruebas-de-error/envio-de-error", token());

        assertThat(respuesta.statusCode()).isEqualTo(503);
        assertThat((Integer) JsonPath.read(respuesta.body(), "$.estado")).isEqualTo(503);
        assertThat(mensajeDe(respuesta)).startsWith("Ocurrió un error inesperado");
        assertThat((String) JsonPath.read(respuesta.body(), "$.ruta")).isEqualTo("/api/pruebas-de-error/envio-de-error");
    }

    @Test
    @DisplayName("Con sesión, una dirección que no existe responde 404 y un método que no corresponde, 405: no 401")
    void direccionOMetodoEquivocados() throws Exception {
        String token = token();

        HttpResponse<String> inexistente = pedir("GET", "/api/no-existe", token);
        assertThat(inexistente.statusCode()).isEqualTo(404);
        assertThat(mensajeDe(inexistente)).isEqualTo("La dirección solicitada no existe");

        HttpResponse<String> metodo = pedir("DELETE", "/api/internos", token);
        assertThat(metodo.statusCode()).isEqualTo(405);
        assertThat(metodo.headers().firstValue("Allow")).isPresent();
    }

    @Test
    @DisplayName("El 401 queda para la sesión que de verdad falta: sin token o con uno inválido")
    void elCuatrocientosUnoQuedaParaLaSesion() throws Exception {
        assertThat(pedir("GET", "/api/pruebas-de-error/excepcion", null).statusCode()).isEqualTo(401);
        assertThat(pedir("GET", "/api/pruebas-de-error/excepcion", "token-inventado").statusCode()).isEqualTo(401);
        // Sin sesión, ni siquiera se averigua si la dirección existe.
        assertThat(pedir("GET", "/api/no-existe", null).statusCode()).isEqualTo(401);
    }

    // ---- apoyo ----

    /** Solo existe en esta prueba: cada método falla de una manera distinta. */
    @RestController
    @RequestMapping("/api/pruebas-de-error")
    public static class ControladorConFallas {

        @GetMapping("/excepcion")
        @SoloMedicoGeneral
        public String excepcion() {
            throw new IllegalStateException(DETALLE_INTERNO);
        }

        /**
         * Lo que pasó el 13/10: una clase que el IDE compiló con errores no
         * lanza una excepción sino un {@link Error}, que no hereda de
         * {@link Exception}.
         */
        @GetMapping("/error-de-java")
        @SoloMedicoGeneral
        public String errorDeJava() {
            // Sin el texto real del IDE, para no confundir a quien busque clases
            // rotas en target/ por ese mensaje.
            throw new Error("Clase compilada con errores (simulada): " + DETALLE_INTERNO);
        }

        /** Una respuesta de error que no pasa por Spring MVC: el contenedor la reenvía a /error. */
        @GetMapping("/envio-de-error")
        @SoloMedicoGeneral
        public void envioDeError(HttpServletResponse respuesta) throws IOException {
            respuesta.sendError(HttpStatus.SERVICE_UNAVAILABLE.value());
        }
    }

    private HttpResponse<String> pedir(String metodo, String ruta, String token) throws Exception {
        HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .method(metodo, HttpRequest.BodyPublishers.noBody());
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        return cliente.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String mensajeDe(HttpResponse<String> respuesta) {
        return JsonPath.read(respuesta.body(), "$.mensaje");
    }

    private String token() throws Exception {
        Usuario usuario = usuarioRepository.findByCorreo(CORREO).orElseGet(Usuario::new);
        usuario.setNombreCompleto("Persona de prueba");
        usuario.setCorreo(CORREO);
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD));
        usuario.setRol(rolRepository.findByNombre(RolNombre.MEDICO_GENERAL).orElseThrow());
        usuario.setActivo(true);
        usuario.setFechaInvalidacionSesiones(null);
        usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);

        HttpRequest login = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"correo\": \"%s\", \"password\": \"%s\"}".formatted(CORREO, PASSWORD)))
                .build();
        HttpResponse<String> respuesta = cliente.send(login, HttpResponse.BodyHandlers.ofString());
        assertThat(respuesta.statusCode()).isEqualTo(200);
        return JsonPath.read(respuesta.body(), "$.token");
    }
}
