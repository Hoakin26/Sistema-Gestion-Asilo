package app.cabezadealgodon.com.gestionasilobackend.models;

/**
 * YS-132. Los estados de la cuenta mensual del familiar, según el diagrama
 * DE-04 del diseño.
 *
 * <p>
 * La cuenta nace en {@code PENDIENTE_PAGO} al consolidar el mes (HU-18). Los
 * pagos (HU-34, RF-22) la mueven: decidido el 20/10, van en HU-34 y no en
 * HU-18, después de los descuentos (HU-19), porque el familiar paga el total
 * con descuento.
 *
 * <p>
 * YS-133. Ningún servicio elige el estado: sale del saldo, de lo pagado y de la
 * fecha de vencimiento ({@code CuentaMensual.estadoAl}), así que no puede
 * quedar uno que contradiga a los montos.
 */
public enum EstadoCuenta {

    /** HU-18. Consolidada; el familiar todavía no paga nada y no ha vencido. */
    PENDIENTE_PAGO,

    /** HU-34. Pagó una parte; el saldo sigue pendiente y no ha vencido. */
    PAGO_PARCIAL,

    /** HU-34. Pagó el total: los servicios incluidos quedan cancelados. */
    COMPLETAMENTE_CANCELADA,

    /** YS-176. Venció (fin del mes siguiente) sin pagarse completa. */
    EN_MORA
}
