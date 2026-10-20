package app.cabezadealgodon.com.gestionasilobackend.models;

import java.util.EnumSet;
import java.util.Set;

/**
 * YS-108. Los estados de la receta, según el diagrama DE-03 del diseño.
 *
 * <pre>
 * PENDIENTE_DESPACHO     ──► ENTREGADA, PARCIALMENTE_ENTREGADA o MEDICAMENTO_AGOTADO
 * MEDICAMENTO_AGOTADO    ──► ENTREGADA o PARCIALMENTE_ENTREGADA
 * PARCIALMENTE_ENTREGADA ──► ENTREGADA o PARCIALMENTE_ENTREGADA
 * </pre>
 *
 * La receta nace en {@code PENDIENTE_DESPACHO}, que es lo que la farmacia ve
 * en su bandeja (HU-15). YS-126: las transiciones viven aquí, como las de la
 * orden (DE-02), y las aplica el despacho (HU-16).
 *
 * <p>
 * El diagrama del análisis lleva la receta de "Entregada Parcialmente" a
 * "Entregada Totalmente" cuando "llega inventario y se completa la entrega":
 * una receta incompleta se vuelve a despachar, y una parcial puede recibir otra
 * entrega parcial antes de completarse. "Medicamento Agotado" lo agregó el
 * diseño (4.6) para el caso en que no se entregó nada por falta de existencias
 * (CU-005, E.1), y sale de ahí de la misma forma.
 */
public enum EstadoReceta {

    /** HU-10. El especialista la registró; la farmacia todavía no entrega nada. */
    PENDIENTE_DESPACHO,

    /** HU-16. Se entregó parte de los medicamentos; los demás siguen pendientes. */
    PARCIALMENTE_ENTREGADA,

    /** HU-16. Se entregaron todos. HU-16 la llama "Despachada". */
    ENTREGADA,

    /** HU-16. No se entregó ninguno por falta de existencias. */
    MEDICAMENTO_AGOTADO;

    /** A qué estados lleva un despacho desde este. */
    public Set<EstadoReceta> siguientes() {
        return switch (this) {
            case PENDIENTE_DESPACHO -> EnumSet.of(PARCIALMENTE_ENTREGADA, ENTREGADA, MEDICAMENTO_AGOTADO);
            case MEDICAMENTO_AGOTADO -> EnumSet.of(PARCIALMENTE_ENTREGADA, ENTREGADA);
            case PARCIALMENTE_ENTREGADA -> EnumSet.of(PARCIALMENTE_ENTREGADA, ENTREGADA);
            // El final: una receta entregada no se vuelve a despachar (HU-16).
            case ENTREGADA -> EnumSet.noneOf(EstadoReceta.class);
        };
    }

    public boolean puedeCambiarA(EstadoReceta nuevo) {
        return siguientes().contains(nuevo);
    }

    /** El estado con los nombres del manual de diseño, para los mensajes de error. */
    public String etiqueta() {
        return switch (this) {
            case PENDIENTE_DESPACHO -> "Pendiente de Despacho";
            case PARCIALMENTE_ENTREGADA -> "Parcialmente Entregada";
            case ENTREGADA -> "Entregada";
            case MEDICAMENTO_AGOTADO -> "Medicamento Agotado";
        };
    }
}
