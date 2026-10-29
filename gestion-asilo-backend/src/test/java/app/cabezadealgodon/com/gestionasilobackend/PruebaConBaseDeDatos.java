package app.cabezadealgodon.com.gestionasilobackend;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;

import app.cabezadealgodon.com.gestionasilobackend.security.ControlIntentosLogin;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/**
 * Base de las pruebas que necesitan base de datos (YS-51).
 *
 * Levanta un PostgreSQL en Docker en vez de usar una base en memoria: así se
 * prueba contra el mismo motor que usa la aplicación, y en vez de apuntar a la
 * base de desarrollo, que obligaría a tenerla encendida y ensuciaría sus datos.
 */
public abstract class PruebaConBaseDeDatos {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:15-alpine");

    static {
        // Se arranca una sola vez para toda la ejecución: un contenedor por
        // clase de prueba multiplicaría el tiempo sin ganar aislamiento real.
        // Testcontainers lo detiene al terminar la máquina virtual.
        POSTGRES.start();
    }

    /**
     * YS-97. El servidor de correo es un simulacro en todas las pruebas: crear
     * una solicitud dispara un aviso al familiar, y ninguna prueba debe
     * depender de que Mailpit esté encendido ni mandar correos reales.
     *
     * <p>
     * Vive aquí y no en la prueba de notificaciones porque así todas las
     * clases comparten un mismo contexto de Spring. Si solo una lo declarara,
     * Spring levantaría un segundo contexto para ella, con su propio arranque.
     */
    @MockitoBean
    protected JavaMailSender correo;

    /**
     * El simulacro devuelve null por defecto, y sobre null no se puede armar
     * un mensaje. Se rehace antes de cada prueba porque Spring limpia el
     * simulacro después de cada una.
     */
    @BeforeEach
    void prepararCorreo() {
        when(correo.createMimeMessage()).thenAnswer(invocacion -> new MimeMessage((Session) null));
    }

    @DynamicPropertySource
    static void conexion(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        // El esquema de las pruebas lo crean las migraciones de Flyway, igual
        // que en producción, y Hibernate solo lo valida. Va fijo aquí para no
        // depender del application.properties local, que no se versiona: si
        // una entidad cambia sin su migración, todas las pruebas lo detectan.
        registro.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        // YS-97. Sin reintentos en segundo plano: una prueba que mira el
        // estado de un aviso no puede tener a otro hilo cambiándolo.
        registro.add("app.notificaciones.reintento-automatico", () -> "false");
        // DEF-02. Para contar cuántas consultas hace una petición.
        registro.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
        registro.add("logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener", () -> "WARN");
    }

    @Autowired
    private ControlIntentosLogin intentosLogin;

    /** DEF-01. Cada prueba empieza sin fallos de inicio de sesión ni bloqueos. */
    @BeforeEach
    void reiniciarIntentosLogin() {
        intentosLogin.limpiar();
    }
}
