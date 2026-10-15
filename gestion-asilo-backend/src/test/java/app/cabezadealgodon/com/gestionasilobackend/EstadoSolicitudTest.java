package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import app.cabezadealgodon.com.gestionasilobackend.exception.TransicionInvalidaException;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoSolicitud;
import app.cabezadealgodon.com.gestionasilobackend.models.SolicitudReferencia;

/**
 * YS-86. El diagrama DE-01 escrito como prueba. No necesita base de datos ni
 * Spring: la regla vive en el enum y en la entidad, que es justamente lo que
 * permite comprobarla así de directo.
 */
@DisplayName("Estados de la solicitud de referencia, DE-01 (YS-86)")
class EstadoSolicitudTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "PENDIENTE_ASIGNACION, CITA_PROGRAMADA",
            "PENDIENTE_ASIGNACION, CANCELADA",
            "CITA_PROGRAMADA, EN_ATENCION",
            "CITA_PROGRAMADA, CANCELADA",
            "EN_ATENCION, ATENDIDA" })
    @DisplayName("Las transiciones del diagrama están permitidas")
    void lasDelDiagramaSePermiten(EstadoSolicitud desde, EstadoSolicitud hacia) {
        assertThat(desde.puedeCambiarA(hacia)).isTrue();
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "PENDIENTE_ASIGNACION, EN_ATENCION",
            "PENDIENTE_ASIGNACION, ATENDIDA",
            "CITA_PROGRAMADA, PENDIENTE_ASIGNACION",
            "EN_ATENCION, CANCELADA",
            "ATENDIDA, PENDIENTE_ASIGNACION",
            "CANCELADA, CITA_PROGRAMADA" })
    @DisplayName("Las que el diagrama no contempla se rechazan")
    void lasDemasSeRechazan(EstadoSolicitud desde, EstadoSolicitud hacia) {
        assertThat(desde.puedeCambiarA(hacia)).isFalse();
    }

    @Test
    @DisplayName("Una solicitud nueva nace pendiente de asignación")
    void naceEnPendiente() {
        assertThat(new SolicitudReferencia().getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE_ASIGNACION);
    }

    @Test
    @DisplayName("La entidad aplica el diagrama: saltarse un paso lanza la excepción del 409")
    void laEntidadRespetaElDiagrama() {
        SolicitudReferencia solicitud = new SolicitudReferencia();
        solicitud.setNumero("REF-2026-0001");

        assertThatThrownBy(() -> solicitud.cambiarEstado(EstadoSolicitud.ATENDIDA))
                .isInstanceOf(TransicionInvalidaException.class);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE_ASIGNACION);

        solicitud.cambiarEstado(EstadoSolicitud.CITA_PROGRAMADA);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.CITA_PROGRAMADA);
    }
}
