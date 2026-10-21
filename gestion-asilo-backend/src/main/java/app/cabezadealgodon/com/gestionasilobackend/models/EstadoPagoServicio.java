package app.cabezadealgodon.com.gestionasilobackend.models;

/**
 * YS-142. Cuánto se ha pagado de una cita pendiente (HU-21, RF-14): los dos
 * estados que pide RF-14. Una cita pagada completa no está pendiente, así que
 * no tiene estado aquí: no aparece en el listado.
 *
 * <p>
 * No se guarda: sale de lo pagado de las cuentas en que se consolidaron sus
 * cargos ({@code ServicioPendienteService}).
 */
public enum EstadoPagoServicio {

    /** Nada pagado: su cuenta no tiene pagos, o su mes no se ha consolidado. */
    SIN_PAGO,

    /** Pagada una parte: su cuenta tiene pagos, pero no cubren el total. */
    PAGO_PARCIAL
}
