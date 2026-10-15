package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;

import app.cabezadealgodon.com.gestionasilobackend.config.RestriccionesDeEnum;

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

    private String definicion() {
        return jdbc.queryForObject("SELECT pg_get_constraintdef(oid) FROM pg_constraint"
                + " WHERE conname = 'notificacion_familiar_tipo_check'", String.class);
    }
}
