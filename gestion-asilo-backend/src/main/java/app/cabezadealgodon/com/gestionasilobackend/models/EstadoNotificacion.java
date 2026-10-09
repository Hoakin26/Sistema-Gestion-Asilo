package app.cabezadealgodon.com.gestionasilobackend.models;

/** YS-95. En qué quedó un aviso al familiar. */
public enum EstadoNotificacion {

    /** Registrado junto con la solicitud; todavía no se intentó enviar. */
    PENDIENTE,

    ENVIADA,

    /** El servidor de correo lo rechazó o no respondió. Se reintenta solo. */
    FALLIDA,

    /**
     * El familiar no tiene correo registrado. No es una falla del envío y no
     * se reintenta solo: no hay a dónde mandarlo. Si alguien agrega el correo,
     * el reintento manual lo vuelve a evaluar.
     */
    SIN_CORREO
}
