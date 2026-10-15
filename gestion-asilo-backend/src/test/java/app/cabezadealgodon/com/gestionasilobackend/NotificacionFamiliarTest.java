package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
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
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoNotificacion;
import app.cabezadealgodon.com.gestionasilobackend.models.Familiar;
import app.cabezadealgodon.com.gestionasilobackend.models.NotificacionFamiliar;
import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.EspecialidadRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.FamiliarRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.NotificacionFamiliarRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.RolRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.SolicitudReferenciaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;
import app.cabezadealgodon.com.gestionasilobackend.services.NotificacionService;
import app.cabezadealgodon.com.gestionasilobackend.services.ReintentoNotificacionesTarea;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;

/**
 * YS-97. El aviso por correo al familiar (HU-04, RF-04): que se envía, qué
 * dice, que un correo caído no se lleva la solicitud, y los dos reintentos.
 *
 * <p>
 * El servidor de correo es el simulacro de {@link PruebaConBaseDeDatos}. El
 * envío corre en otro hilo después de confirmar la solicitud, así que cada
 * prueba espera a que el aviso cambie de estado en vez de mirarlo enseguida.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Aviso por correo al familiar (YS-97)")
class NotificacionFamiliarTest extends PruebaConBaseDeDatos {

    private static final String PASSWORD = "Prueba1234";
    private static final String MOTIVO = "Pérdida de memoria reciente que avanza";
    private static final AtomicLong SIGUIENTE_DPI = new AtomicLong(6_300_000_000_000L);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Autowired
    private FamiliarRepository familiarRepository;

    @Autowired
    private SolicitudReferenciaRepository solicitudRepository;

    @Autowired
    private NotificacionFamiliarRepository notificacionRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Crear una solicitud le escribe al familiar con el número, la especialidad y el estado")
    void avisaAlFamiliarAlCrear() throws Exception {
        Long solicitudId = crearSolicitud("familia.aviso@ejemplo.com");

        esperarEstado(solicitudId, EstadoNotificacion.ENVIADA);

        MimeMessage mensaje = mensajeA("familia.aviso@ejemplo.com");
        String numero = solicitudRepository.findById(solicitudId).orElseThrow().getNumero();
        String texto = textoPlano(mensaje);

        assertThat(destinatarios(mensaje)).containsExactly("familia.aviso@ejemplo.com");
        assertThat(mensaje.getSubject()).contains(numero);
        assertThat(texto).contains(numero, "Cardiología", "Pendiente de asignación");
    }

    /**
     * RNF-03. El correo informa la referencia pero no el motivo, que es un
     * dato clínico: una vez enviado, el asilo ya no controla quién lo lee.
     */
    @Test
    @DisplayName("El correo no incluye el motivo de la referencia, que es un dato clínico")
    void elCorreoNoLlevaElMotivo() throws Exception {
        Long solicitudId = crearSolicitud("familia.privacidad@ejemplo.com");
        esperarEstado(solicitudId, EstadoNotificacion.ENVIADA);

        assertThat(textoPlano(mensajeA("familia.privacidad@ejemplo.com"))).doesNotContain(MOTIVO);
    }

    @Test
    @DisplayName("Con el servidor de correo caído, la solicitud se guarda igual y el aviso queda fallido")
    void laSolicitudSobreviveAlCorreoCaido() throws Exception {
        doThrow(new MailSendException("Servidor de correo caído")).when(correo).send(any(MimeMessage.class));

        Long solicitudId = crearSolicitud("familia.caida@ejemplo.com");

        NotificacionFamiliar aviso = esperarEstado(solicitudId, EstadoNotificacion.FALLIDA);
        assertThat(aviso.getIntentos()).isEqualTo(1);
        assertThat(solicitudRepository.existsById(solicitudId)).isTrue();
    }

    @Test
    @DisplayName("Un familiar sin correo no es una falla: queda 'sin correo' y no se intenta enviar")
    void sinCorreoNoEsFalla() throws Exception {
        Long solicitudId = crearSolicitud(null);

        esperarEstado(solicitudId, EstadoNotificacion.SIN_CORREO);
        verify(correo, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("La respuesta de la solicitud dice si el familiar recibirá aviso")
    void laRespuestaDiceSiHabraAviso() throws Exception {
        String sinCorreo = crearSolicitudRespuesta(null);
        String conCorreo = crearSolicitudRespuesta("familia.respuesta@ejemplo.com");

        assertThat((Boolean) JsonPath.read(sinCorreo, "$.familiarConCorreo")).isFalse();
        assertThat((Boolean) JsonPath.read(conCorreo, "$.familiarConCorreo")).isTrue();

        // Se espera el envío: si quedara en curso, caería en la prueba siguiente.
        esperarEstado(((Number) JsonPath.read(conCorreo, "$.id")).longValue(), EstadoNotificacion.ENVIADA);
    }

    @Test
    @DisplayName("El reintento manual envía el aviso fallido")
    void elReintentoManualEnviaElFallido() throws Exception {
        doThrow(new MailSendException("Servidor de correo caído")).when(correo).send(any(MimeMessage.class));
        Long solicitudId = crearSolicitud("familia.manual@ejemplo.com");
        esperarEstado(solicitudId, EstadoNotificacion.FALLIDA);

        // El servidor vuelve.
        doNothing().when(correo).send(any(MimeMessage.class));

        mvc.perform(post("/api/solicitudes-referencia/{id}/notificaciones/reintentar", solicitudId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_FUNDACION)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("ENVIADA"))
                .andExpect(jsonPath("$[0].intentos").value(2));
    }

    /**
     * Cada intento lee el correo actual del familiar. Si no tenía, o estaba
     * mal escrito, y alguien lo corrige, el reintento ya va al correcto.
     */
    @Test
    @DisplayName("Si se agrega el correo del familiar, el reintento manual lo usa")
    void elReintentoUsaElCorreoCorregido() throws Exception {
        Long solicitudId = crearSolicitud(null);
        esperarEstado(solicitudId, EstadoNotificacion.SIN_CORREO);

        Familiar familiar = solicitudRepository.findById(solicitudId).orElseThrow()
                .getInterno().getFamiliarResponsable();
        familiar.setCorreo("familia.corregida@ejemplo.com");
        familiarRepository.save(familiar);

        mvc.perform(post("/api/solicitudes-referencia/{id}/notificaciones/reintentar", solicitudId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.MEDICO_GENERAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("ENVIADA"));

        assertThat(destinatarios(mensajeA("familia.corregida@ejemplo.com")))
                .containsExactly("familia.corregida@ejemplo.com");
    }

    @Test
    @DisplayName("El reintento automático envía los fallidos, pero no los que agotaron sus intentos")
    void elReintentoAutomaticoRespetaElMaximo() throws Exception {
        doThrow(new MailSendException("Servidor de correo caído")).when(correo).send(any(MimeMessage.class));
        Long reintentable = crearSolicitud("familia.auto@ejemplo.com");
        Long agotado = crearSolicitud("familia.agotada@ejemplo.com");
        esperarEstado(reintentable, EstadoNotificacion.FALLIDA);
        NotificacionFamiliar avisoAgotado = esperarEstado(agotado, EstadoNotificacion.FALLIDA);
        avisoAgotado.setIntentos(5);
        notificacionRepository.save(avisoAgotado);

        doNothing().when(correo).send(any(MimeMessage.class));
        // La tarea está apagada en las pruebas; se la invoca a mano.
        new ReintentoNotificacionesTarea(notificacionService).reintentar();

        assertThat(avisoDe(reintentable).getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(avisoDe(agotado).getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
        assertThat(avisoDe(agotado).getIntentos()).isEqualTo(5);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RolNombre.class, names = { "MEDICO_GENERAL", "ADMINISTRADOR_FUNDACION" },
            mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Los avisos los consultan y reintentan solo el Médico General y la Fundación")
    void losAvisosSonDeQuienDaSeguimiento(RolNombre rol) throws Exception {
        Long solicitudId = crearSolicitud(null);
        String token = tokenDe(rol);

        mvc.perform(get("/api/solicitudes-referencia/{id}/notificaciones", solicitudId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/solicitudes-referencia/{id}/notificaciones/reintentar", solicitudId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("La consulta de avisos no expone el correo del familiar")
    void laConsultaNoExponeElCorreo() throws Exception {
        Long solicitudId = crearSolicitud("familia.oculta@ejemplo.com");
        esperarEstado(solicitudId, EstadoNotificacion.ENVIADA);

        mvc.perform(get("/api/solicitudes-referencia/{id}/notificaciones", solicitudId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.ADMINISTRADOR_FUNDACION)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("ENVIADA"))
                .andExpect(jsonPath("$[0].destinatario").doesNotExist())
                .andExpect(jsonPath("$[0].ultimoError").doesNotExist());
    }

    @Test
    @DisplayName("Los avisos de una solicitud que no existe responden 404")
    void solicitudInexistente() throws Exception {
        mvc.perform(get("/api/solicitudes-referencia/{id}/notificaciones", 999_999)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.MEDICO_GENERAL)))
                .andExpect(status().isNotFound());
    }

    // ---- apoyo ----

    private NotificacionFamiliar avisoDe(Long solicitudId) {
        return notificacionRepository.findBySolicitudIdOrderByIdAsc(solicitudId).get(0);
    }

    /** El envío corre en otro hilo: se espera a que el aviso llegue al estado. */
    private NotificacionFamiliar esperarEstado(Long solicitudId, EstadoNotificacion estado) {
        await().atMost(Duration.ofSeconds(10))
                .until(() -> !notificacionRepository.findBySolicitudIdOrderByIdAsc(solicitudId).isEmpty()
                        && avisoDe(solicitudId).getEstado() == estado);
        return avisoDe(solicitudId);
    }

    private Long crearSolicitud(String correoFamiliar) throws Exception {
        return ((Number) JsonPath.read(crearSolicitudRespuesta(correoFamiliar), "$.id")).longValue();
    }

    private String crearSolicitudRespuesta(String correoFamiliar) throws Exception {
        String cuerpo = """
                {"internoId": %d, "especialidadId": %d, "enfermeroAcompananteId": %d, "motivo": "%s"}
                """.formatted(
                registrarInterno(correoFamiliar),
                especialidadRepository.findByActivaTrueOrderByNombreAsc().get(0).getId(),
                usuarioDe(RolNombre.ENFERMERIA).getId(),
                MOTIVO);

        return mvc.perform(post("/api/solicitudes-referencia")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.MEDICO_GENERAL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private Long registrarInterno(String correoFamiliar) throws Exception {
        String correo = correoFamiliar == null ? "" : ", \"correo\": \"%s\"".formatted(correoFamiliar);
        String cuerpo = """
                {"nombreCompleto": "Interno con familiar avisado",
                 "dpi": "%d",
                 "fechaNacimiento": "1938-04-02",
                 "fechaIngreso": "2022-09-15",
                 "familiarResponsable": {
                   "nombreCompleto": "Familiar de prueba",
                   "parentesco": "Hijo",
                   "telefono": "5555 0000"%s}}
                """.formatted(SIGUIENTE_DPI.getAndIncrement(), correo);

        String respuesta = mvc.perform(post("/api/internos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(RolNombre.MEDICO_GENERAL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    /**
     * El mensaje enviado a una dirección. Se buscan entre todos los envíos y no
     * se exige que haya uno solo: así la prueba no depende de lo que otra deje
     * en curso.
     */
    private MimeMessage mensajeA(String direccion) throws Exception {
        ArgumentCaptor<MimeMessage> enviados = ArgumentCaptor.forClass(MimeMessage.class);
        verify(correo, atLeastOnce()).send(enviados.capture());
        for (MimeMessage mensaje : enviados.getAllValues()) {
            if (java.util.Arrays.asList(destinatarios(mensaje)).contains(direccion)) {
                return mensaje;
            }
        }
        throw new AssertionError("No se envió ningún correo a " + direccion);
    }

    /** El texto plano del mensaje, buscándolo entre las partes del multipart. */
    private static String textoPlano(Part parte) throws Exception {
        // Se mira el contenido y no solo el tipo declarado: un mensaje que el
        // simulacro "envió" nunca pasó por saveChanges, no tiene Content-Type y
        // dice ser text/plain aunque por dentro sea multipart.
        Object contenido = parte.getContent();
        if (contenido instanceof String texto) {
            return parte.isMimeType("text/plain") ? texto : null;
        }
        if (contenido instanceof Multipart partes) {
            for (int i = 0; i < partes.getCount(); i++) {
                BodyPart hija = partes.getBodyPart(i);
                String texto = textoPlano(hija);
                if (texto != null) return texto;
            }
        }
        return null;
    }

    private static String[] destinatarios(MimeMessage mensaje) throws Exception {
        Address[] direcciones = mensaje.getRecipients(Message.RecipientType.TO);
        String[] texto = new String[direcciones.length];
        for (int i = 0; i < direcciones.length; i++) {
            texto[i] = direcciones[i].toString();
        }
        return texto;
    }

    private Usuario usuarioDe(RolNombre rol) {
        String correo = rol.name().toLowerCase(Locale.ROOT) + ".avisos@asilo.gt";

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
