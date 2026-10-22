package app.cabezadealgodon.com.gestionasilobackend.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoCuenta;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoNotificacion;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoOrden;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoReceta;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoSolicitud;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoVisita;
import app.cabezadealgodon.com.gestionasilobackend.models.FormaPago;
import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoArchivoResultado;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoDescuento;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoDonante;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoGasto;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoNotificacion;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;

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
 *
 * <p>
 * YS-183: la lista tiene todas las columnas enum de las entidades, menos los
 * días de la semana de los horarios, que no van a crecer. Hasta el Sprint 3
 * solo tenía las de los avisos, las solicitudes y el laboratorio; las demás
 * se quedaban como nacieron. {@code RestriccionesDeEnumTest} recorre las
 * entidades y falla si alguna falta.
 */
@Component
@Order(0)
public class RestriccionesDeEnum implements CommandLineRunner {

    /** El nombre es el que Postgres le da a la restricción que crea Hibernate. */
    public record Restriccion(String tabla, String columna, Class<? extends Enum<?>> tipo) {
        String nombre() {
            return tabla + "_" + columna + "_check";
        }
    }

    public static final List<Restriccion> RESTRICCIONES = List.of(
            new Restriccion("notificacion_familiar", "tipo", TipoNotificacion.class),
            new Restriccion("notificacion_familiar", "estado", EstadoNotificacion.class),
            new Restriccion("solicitud_referencia", "estado", EstadoSolicitud.class),
            // HU-13 (YS-117): el estado de la orden y el tipo de archivo de su resultado.
            new Restriccion("orden_laboratorio", "estado", EstadoOrden.class),
            new Restriccion("orden_laboratorio", "tipo_archivo", TipoArchivoResultado.class),
            // YS-183: las que faltaban, de los Sprints 1 a 3.
            new Restriccion("rol", "nombre", RolNombre.class),
            new Restriccion("visita_medica", "estado", EstadoVisita.class),
            new Restriccion("receta", "estado", EstadoReceta.class),
            new Restriccion("cargo_servicio", "tipo", TipoServicio.class),
            new Restriccion("cuenta_mensual", "estado", EstadoCuenta.class),
            new Restriccion("politica_descuento", "tipo_servicio", TipoServicio.class),
            new Restriccion("politica_descuento", "tipo_descuento", TipoDescuento.class),
            new Restriccion("pago", "forma_pago", FormaPago.class),
            new Restriccion("cuenta_por_pagar", "tipo_servicio", TipoServicio.class),
            new Restriccion("pago_fundacion", "forma_pago", FormaPago.class),
            new Restriccion("donacion", "tipo_donante", TipoDonante.class),
            new Restriccion("gasto_operativo", "tipo_gasto", TipoGasto.class));

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
