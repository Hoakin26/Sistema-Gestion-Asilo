package app.cabezadealgodon.com.gestionasilobackend;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

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

    @DynamicPropertySource
    static void conexion(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
