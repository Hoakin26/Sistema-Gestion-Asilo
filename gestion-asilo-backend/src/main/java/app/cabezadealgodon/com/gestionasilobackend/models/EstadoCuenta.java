package app.cabezadealgodon.com.gestionasilobackend.models;

/**
 * YS-132. Los estados de la cuenta mensual del familiar, según el diagrama
 * DE-04 del diseño.
 *
 * <p>
 * La cuenta nace en {@code PENDIENTE_PAGO} al consolidar el mes (HU-18). Las
 * transiciones las agrega el registro de pagos (HU-34, RF-22): hasta entonces,
 * nada saca a una cuenta de su estado inicial. Decidido el 20/10: los pagos no
 * van en HU-18 sino en HU-34, que llega después de los descuentos (HU-19)
 * porque el familiar paga el total con descuento.
 */
public enum EstadoCuenta {

    /** HU-18. Consolidada; el familiar todavía no paga nada. */
    PENDIENTE_PAGO,

    /** HU-34. Pagó una parte; el saldo sigue pendiente. */
    PAGO_PARCIAL,

    /** HU-34. Pagó el total: los servicios incluidos quedan cancelados. */
    COMPLETAMENTE_CANCELADA,

    /** HU-34. Venció sin pagarse completa. */
    EN_MORA
}
