package app.cabezadealgodon.com.gestionasilobackend.models;

/**
 * YS-133. Cómo pagó el familiar (HU-34, RF-22): lo que el asilo recibe en la
 * práctica. Los reportes de cobros y de entradas (HU-26, HU-27) lo agrupan
 * por aquí.
 */
public enum FormaPago {
    EFECTIVO,
    TRANSFERENCIA,
    DEPOSITO,
    TARJETA,
    CHEQUE
}
