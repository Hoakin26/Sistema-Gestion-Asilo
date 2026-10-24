package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.jdbc.core.JdbcTemplate;

import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Las restricciones CHECK de las columnas enum admiten todos los valores de su
 * enum.
 *
 * <p>
 * Cada {@code @Enumerated(STRING)} tiene en la base una restricción con la
 * lista de valores permitidos, y esa lista la escriben las migraciones de
 * Flyway, no Hibernate. Si alguien agrega un valor a un enum y olvida la
 * migración que lo agrega a la restricción, la aplicación compila, arranca y
 * pasa la validación del esquema, pero guardar ese valor responde 500 (le pasó
 * a HU-06 con {@code CITA_ASIGNADA}). Esta prueba lo detecta antes.
 *
 * <p>
 * Las columnas salen de las anotaciones de las entidades (YS-183), también las
 * heredadas de una {@code @MappedSuperclass}: una columna enum nueva queda
 * cubierta sin tocar esta prueba. Reemplaza a {@code RestriccionesDeEnum} y su
 * prueba, que rehacían las restricciones al arrancar; con Flyway el esquema
 * solo cambia por migraciones. Los días de la semana de los horarios no se
 * revisan: no van a crecer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Restricciones de las columnas enum al día con su enum")
class EnumsEnLaBaseTest extends PruebaConBaseDeDatos {

    record ColumnaEnum(String tabla, String columna, Class<?> tipo) {
        @Override
        public String toString() {
            return tabla + "." + columna;
        }
    }

    @Autowired
    private JdbcTemplate jdbc;

    static List<ColumnaEnum> columnas() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider escaner = new ClassPathScanningCandidateComponentProvider(false);
        escaner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        List<ColumnaEnum> columnas = new ArrayList<>();
        for (BeanDefinition definicion : escaner.findCandidateComponents(Usuario.class.getPackageName())) {
            Class<?> entidad = Class.forName(definicion.getBeanClassName());
            Table anotacion = entidad.getAnnotation(Table.class);
            String tabla = anotacion != null && !anotacion.name().isEmpty() ? anotacion.name()
                    : enSnakeCase(entidad.getSimpleName());
            for (Class<?> clase = entidad; clase != Object.class; clase = clase.getSuperclass()) {
                for (Field campo : clase.getDeclaredFields()) {
                    Enumerated enumerado = campo.getAnnotation(Enumerated.class);
                    if (enumerado == null || enumerado.value() != EnumType.STRING
                            || campo.getType() == DayOfWeek.class) {
                        continue;
                    }
                    Column columna = campo.getAnnotation(Column.class);
                    columnas.add(new ColumnaEnum(tabla,
                            columna != null && !columna.name().isEmpty() ? columna.name() : enSnakeCase(campo.getName()),
                            campo.getType()));
                }
            }
        }
        return columnas;
    }

    /** "tipoServicio" → "tipo_servicio", como la estrategia de nombres de Spring Boot. */
    private static String enSnakeCase(String nombre) {
        return nombre.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("columnas")
    @DisplayName("La restricción admite cada valor del enum")
    void admiteTodosLosValores(ColumnaEnum columna) {
        String definicion = jdbc.queryForObject(
                "SELECT string_agg(pg_get_constraintdef(c.oid), ' ') FROM pg_constraint c"
                        + " JOIN pg_class t ON t.oid = c.conrelid"
                        + " WHERE c.contype = 'c' AND t.relname = ?"
                        + " AND pg_get_constraintdef(c.oid) LIKE ?",
                String.class, columna.tabla(), "%" + columna.columna() + "%");

        assertThat(definicion)
                .as("restricción CHECK de %s; si falta un valor, agréguelo con una migración nueva", columna)
                .isNotNull()
                .contains(Arrays.stream(columna.tipo().getEnumConstants())
                        .map(valor -> "'" + ((Enum<?>) valor).name() + "'")
                        .toList());
    }
}
