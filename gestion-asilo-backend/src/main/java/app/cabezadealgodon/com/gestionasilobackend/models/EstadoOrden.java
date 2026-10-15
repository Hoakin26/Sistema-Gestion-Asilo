package app.cabezadealgodon.com.gestionasilobackend.models;

/**
 * YS-111. Los estados de la orden de laboratorio, según el diagrama DE-02 del
 * diseño.
 *
 * <p>
 * La orden nace en {@code PENDIENTE}, que es lo que el laboratorio ve en su
 * bandeja (HU-12). Las transiciones las agregan las historias del
 * laboratorio, que son las que la mueven (HU-12 y HU-13).
 */
public enum EstadoOrden {

    /** HU-11. El especialista la ordenó; el laboratorio todavía no la toma. */
    PENDIENTE,

    /** El laboratorio la está procesando. */
    EN_PROCESO,

    /** HU-13. El resultado ya está registrado. HU-13 lo llama "Completada". */
    RESULTADOS_CARGADOS,

    /** Se anuló antes de realizarse. */
    ANULADA
}
