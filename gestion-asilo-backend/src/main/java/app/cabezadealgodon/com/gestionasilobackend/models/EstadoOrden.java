package app.cabezadealgodon.com.gestionasilobackend.models;

import java.util.EnumSet;
import java.util.Set;

/**
 * YS-111. Los estados de la orden de laboratorio, según el diagrama DE-02 del
 * diseño.
 *
 * <pre>
 * PENDIENTE  ──► EN_PROCESO, RESULTADOS_CARGADOS o ANULADA
 * EN_PROCESO ──► RESULTADOS_CARGADOS o ANULADA
 * </pre>
 *
 * La orden nace en {@code PENDIENTE}, que es lo que el laboratorio ve en su
 * bandeja (HU-12). YS-114: las transiciones viven aquí, igual que las de la
 * solicitud (DE-01), para que ningún servicio pueda devolver a pendiente una
 * orden que ya tiene resultado.
 */
public enum EstadoOrden {

    /** HU-11. El especialista la ordenó; el laboratorio todavía no la toma. */
    PENDIENTE,

    /** HU-12. El laboratorio la tomó y la está procesando. */
    EN_PROCESO,

    /** HU-13. El resultado ya está registrado. HU-13 lo llama "Completada". */
    RESULTADOS_CARGADOS,

    /** Se anuló antes de realizarse. */
    ANULADA;

    /**
     * A qué estados se puede pasar desde este. El resultado se acepta también
     * sobre una orden pendiente, como dicen las reglas de negocio del diseño:
     * un examen rápido se toma y se registra sin pasar por "En Proceso".
     */
    public Set<EstadoOrden> siguientes() {
        return switch (this) {
            case PENDIENTE -> EnumSet.of(EN_PROCESO, RESULTADOS_CARGADOS, ANULADA);
            case EN_PROCESO -> EnumSet.of(RESULTADOS_CARGADOS, ANULADA);
            // Los dos finales: de ahí no se sale.
            case RESULTADOS_CARGADOS, ANULADA -> EnumSet.noneOf(EstadoOrden.class);
        };
    }

    public boolean puedeCambiarA(EstadoOrden nuevo) {
        return siguientes().contains(nuevo);
    }

    /** El estado con los nombres del manual de diseño, para los mensajes de error. */
    public String etiqueta() {
        return switch (this) {
            case PENDIENTE -> "Pendiente";
            case EN_PROCESO -> "En Proceso";
            case RESULTADOS_CARGADOS -> "Resultados Cargados";
            case ANULADA -> "Anulada";
        };
    }
}
