package app.cabezadealgodon.com.gestionasilobackend.models;

/**
 * YS-139. El estado de una deuda del asilo con la fundación (HU-20): los dos
 * del criterio de aceptación. Como el de la cuenta mensual, no se elige: sale
 * del saldo ({@code CuentaPorPagarService}). Una deuda pagada vuelve a
 * Pendiente si después llega un cargo nuevo de ese mes (decidido el 21/10).
 */
public enum EstadoCuentaPorPagar {

    /** Queda saldo: lo adeudado es más que lo pagado. */
    PENDIENTE,

    /** Sin saldo: lo pagado cubre todo lo adeudado. */
    PAGADA
}
