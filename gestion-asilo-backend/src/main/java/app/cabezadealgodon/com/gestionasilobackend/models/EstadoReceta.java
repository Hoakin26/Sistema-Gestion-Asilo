package app.cabezadealgodon.com.gestionasilobackend.models;

/**
 * YS-108. Los estados de la receta, según el diagrama DE-03 del diseño.
 *
 * <p>
 * La receta nace en {@code PENDIENTE_DESPACHO}, que es lo que la farmacia ve
 * en su bandeja (HU-15). Las transiciones las agrega HU-16, que es la que
 * despacha: hasta entonces, nada saca a una receta de su estado inicial.
 */
public enum EstadoReceta {

    /** HU-10. El especialista la registró; la farmacia todavía no entrega nada. */
    PENDIENTE_DESPACHO,

    /** HU-16. Se entregó parte de los medicamentos. */
    PARCIALMENTE_ENTREGADA,

    /** HU-16. Se entregaron todos. */
    ENTREGADA,

    /** HU-16. La farmacia no tiene algún medicamento. */
    MEDICAMENTO_AGOTADO
}
