package app.cabezadealgodon.com.gestionasilobackend.models;

import java.util.EnumSet;
import java.util.Set;

/**
 * YS-83. Los estados de la solicitud de referencia, según el diagrama DE-01
 * (sección 3.5 del diseño).
 *
 * <pre>
 * PENDIENTE_ASIGNACION ──► CITA_PROGRAMADA ──► EN_ATENCION ──► ATENDIDA
 *          │                     │
 *          └──────► CANCELADA ◄──┘
 * </pre>
 *
 * Las transiciones viven aquí y no en cada servicio que cambia el estado: así
 * la regla se escribe una sola vez, y ningún servicio puede devolver una
 * solicitud atendida a pendiente aunque lo intente.
 */
public enum EstadoSolicitud {

    /** HU-03. El médico general la creó; espera que la fundación le asigne cita. */
    PENDIENTE_ASIGNACION,

    /** HU-06. La fundación asignó especialista, fecha y hora. */
    CITA_PROGRAMADA,

    /** HU-07. El especialista inició la visita. */
    EN_ATENCION,

    /** HU-09. La visita se finalizó con diagnóstico. */
    ATENDIDA,

    CANCELADA;

    /**
     * A qué estados se puede pasar desde este. Un {@code switch} y no un mapa
     * estático: en un enum, un mapa que se refiere a sus propias constantes se
     * inicializa antes que ellas y queda lleno de nulos.
     */
    public Set<EstadoSolicitud> siguientes() {
        return switch (this) {
            case PENDIENTE_ASIGNACION -> EnumSet.of(CITA_PROGRAMADA, CANCELADA);
            case CITA_PROGRAMADA -> EnumSet.of(EN_ATENCION, CANCELADA);
            case EN_ATENCION -> EnumSet.of(ATENDIDA);
            // Los dos finales: de ahí no se sale.
            case ATENDIDA, CANCELADA -> EnumSet.noneOf(EstadoSolicitud.class);
        };
    }

    public boolean puedeCambiarA(EstadoSolicitud nuevo) {
        return siguientes().contains(nuevo);
    }

    /**
     * YS-94. El estado como lo lee una persona, con los nombres del manual de
     * diseño. Lo usa el correo al familiar, que no puede mostrarle
     * "PENDIENTE_ASIGNACION".
     */
    public String etiqueta() {
        return switch (this) {
            case PENDIENTE_ASIGNACION -> "Pendiente de asignación";
            case CITA_PROGRAMADA -> "Cita programada";
            case EN_ATENCION -> "En atención";
            case ATENDIDA -> "Atendida";
            case CANCELADA -> "Cancelada";
        };
    }
}
