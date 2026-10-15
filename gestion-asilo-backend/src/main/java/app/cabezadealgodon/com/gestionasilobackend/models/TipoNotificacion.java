package app.cabezadealgodon.com.gestionasilobackend.models;

/**
 * YS-94. Por qué se le escribe al familiar.
 *
 * <p>
 * El RF-04 pide informar el estado y el especialista asignado. Al crear la
 * solicitud todavía no hay especialista, así que el aviso va en dos momentos.
 */
public enum TipoNotificacion {

    /** HU-04 (YS-94). La referencia, su especialidad y su estado. */
    SOLICITUD_CREADA,

    /** HU-06 (YS-91). El especialista, la fecha y la hora de la cita. */
    CITA_ASIGNADA
}
