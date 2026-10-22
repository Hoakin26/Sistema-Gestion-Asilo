package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.jdbc.core.JdbcTemplate;

import app.cabezadealgodon.com.gestionasilobackend.config.RestriccionesDeEnum;
import app.cabezadealgodon.com.gestionasilobackend.config.RestriccionesDeEnum.Restriccion;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * HU-06. Una base creada antes de que un enum creciera no rechaza los valores
 * nuevos. Las demás pruebas no pueden verlo: su esquema nace con el enum de
 * hoy. Aquí se recrea a mano la restricción como la dejó HU-04.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Restricciones de las columnas enum al día con su enum (HU-06)")
class RestriccionesDeEnumTest extends PruebaConBaseDeDatos {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RestriccionesDeEnum restricciones;

    @Test
    @DisplayName("La restricción de HU-04, que solo admitía SOLICITUD_CREADA, se rehace con CITA_ASIGNADA")
    void rehaceLaRestriccionVieja() {
        // NOT VALID: otras pruebas ya dejaron avisos de cita en la tabla, y la
        // restricción vieja no tiene por qué revisarlos para esta prueba.
        jdbc.execute("ALTER TABLE notificacion_familiar"
                + " DROP CONSTRAINT IF EXISTS notificacion_familiar_tipo_check,"
                + " ADD CONSTRAINT notificacion_familiar_tipo_check CHECK (tipo = 'SOLICITUD_CREADA') NOT VALID");
        assertThat(definicion()).doesNotContain("CITA_ASIGNADA");

        restricciones.run();

        assertThat(definicion()).contains("SOLICITUD_CREADA", "CITA_ASIGNADA");
    }

    /**
     * YS-183. Si una entidad trae una columna enum que no está en la lista, su
     * restricción se queda como nació, y el día que el enum crezca guardar el
     * valor nuevo responde 500. Aquí se leen las anotaciones de las entidades,
     * también las que heredan de una {@code @MappedSuperclass}.
     */
    @Test
    @DisplayName("Toda columna enum de las entidades está en la lista de restricciones, salvo los días de la semana")
    void todaColumnaEnumEstaEnLaLista() throws ClassNotFoundException {
        assertThat(RestriccionesDeEnum.RESTRICCIONES).containsExactlyInAnyOrderElementsOf(columnasEnum());
    }

    private static Set<Restriccion> columnasEnum() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider escaner = new ClassPathScanningCandidateComponentProvider(false);
        escaner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        Set<Restriccion> columnas = new HashSet<>();
        for (BeanDefinition definicion : escaner.findCandidateComponents(Usuario.class.getPackageName())) {
            Class<?> entidad = Class.forName(definicion.getBeanClassName());
            Table anotacion = entidad.getAnnotation(Table.class);
            String tabla = anotacion != null && !anotacion.name().isEmpty() ? anotacion.name()
                    : enSnakeCase(entidad.getSimpleName());
            for (Class<?> clase = entidad; clase != Object.class; clase = clase.getSuperclass()) {
                for (Field campo : clase.getDeclaredFields()) {
                    Enumerated enumerado = campo.getAnnotation(Enumerated.class);
                    // Los días de la semana no van a crecer.
                    if (enumerado == null || enumerado.value() != EnumType.STRING
                            || campo.getType() == DayOfWeek.class) {
                        continue;
                    }
                    Column columna = campo.getAnnotation(Column.class);
                    @SuppressWarnings("unchecked")
                    Class<? extends Enum<?>> tipo = (Class<? extends Enum<?>>) campo.getType();
                    columnas.add(new Restriccion(tabla,
                            columna != null && !columna.name().isEmpty() ? columna.name() : enSnakeCase(campo.getName()),
                            tipo));
                }
            }
        }
        return columnas;
    }

    /** "tipoServicio" → "tipo_servicio", como la estrategia de nombres de Spring Boot. */
    private static String enSnakeCase(String nombre) {
        return nombre.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private String definicion() {
        return jdbc.queryForObject("SELECT pg_get_constraintdef(oid) FROM pg_constraint"
                + " WHERE conname = 'notificacion_familiar_tipo_check'", String.class);
    }
}
