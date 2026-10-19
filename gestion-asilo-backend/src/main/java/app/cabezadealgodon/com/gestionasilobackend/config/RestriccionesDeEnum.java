package app.cabezadealgodon.com.gestionasilobackend.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoNotificacion;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoOrden;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoSolicitud;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoArchivoResultado;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoNotificacion;

/**
 * HU-06. Pone al día las restricciones CHECK de las columnas enum.
 *
 * <p>
 * Para cada {@code @Enumerated(STRING)}, Hibernate crea una restricción que
 * admite solo los valores que el enum tiene en ese momento, y con
 * {@code ddl-auto=update} no la vuelve a tocar. Cuando HU-06 agregó
 * {@code CITA_ASIGNADA} a {@link TipoNotificacion}, la base de desarrollo
 * siguió aceptando solo {@code SOLICITUD_CREADA}, y asignar una cita respondía
 * 500 al registrar el aviso al familiar. Las pruebas no lo veían porque crean
 * el esquema desde cero.
 *
 * <p>
 * Aquí, al arrancar, se rehace cada una con los valores que el enum tiene hoy:
 * agregar un valor ya no exige tocar la base a mano. Va antes que los
 * sembradores. Toda columna enum que pueda crecer se anota en la lista.
 */
@Component
@Order(0)
public class RestriccionesDeEnum implements CommandLineRunner {

    /** El nombre es el que Postgres le da a la restricción que crea Hibernate. */
    record Restriccion(String tabla, String columna, Class<? extends Enum<?>> tipo) {
        String nombre() {
            return tabla + "_" + columna + "_check";
        }
    }

    static final List<Restriccion> RESTRICCIONES = List.of(
            new Restriccion("notificacion_familiar", "tipo", TipoNotificacion.class),
            new Restriccion("notificacion_familiar", "estado", EstadoNotificacion.class),
            new Restriccion("solicitud_referencia", "estado", EstadoSolicitud.class),
            // HU-13 (YS-117): el estado de la orden y el tipo de archivo de su resultado.
            new Restriccion("orden_laboratorio", "estado", EstadoOrden.class),
            new Restriccion("orden_laboratorio", "tipo_archivo", TipoArchivoResultado.class));

    private final JdbcTemplate jdbc;

    public RestriccionesDeEnum(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /*
     * Quitar y volver a poner en una sola sentencia: si poner la nueva fallara,
     * la vieja no queda borrada. Se arma concatenando porque los nombres y los
     * valores salen del código, nunca de lo que escribe un usuario.
     */
    @Override
    public void run(String... args) {
        for (Restriccion restriccion : RESTRICCIONES) {
            String valores = Arrays.stream(restriccion.tipo().getEnumConstants())
                    .map(valor -> "'" + valor.name() + "'")
                    .collect(Collectors.joining(", "));
            jdbc.execute("ALTER TABLE " + restriccion.tabla()
                    + " DROP CONSTRAINT IF EXISTS " + restriccion.nombre()
                    + ", ADD CONSTRAINT " + restriccion.nombre()
                    + " CHECK (" + restriccion.columna() + " IN (" + valores + "))");
        }
    }
}
