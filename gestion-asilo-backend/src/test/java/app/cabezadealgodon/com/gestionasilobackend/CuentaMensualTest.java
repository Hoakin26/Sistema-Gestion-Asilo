package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import app.cabezadealgodon.com.gestionasilobackend.models.CuentaMensual;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoCuenta;

/**
 * YS-178. El estado de la cuenta mensual (DE-04, HU-34), sin base ni Spring:
 * cuándo vence, cuándo está en mora y cómo la mueven los pagos y sus
 * anulaciones.
 */
@DisplayName("Estado de la cuenta mensual del familiar (YS-178)")
class CuentaMensualTest {

    /** El último día para pagar la cuenta de octubre de 2026. */
    private static final LocalDate VENCE = LocalDate.of(2026, 11, 30);

    @Test
    @DisplayName("Vence el último día del mes siguiente, también al cambiar de año y en febrero bisiesto")
    void fechaDeVencimiento() {
        assertThat(cuenta("2026-10", "100").getFechaVencimiento()).isEqualTo(VENCE);
        assertThat(cuenta("2026-12", "100").getFechaVencimiento()).isEqualTo(LocalDate.of(2027, 1, 31));
        assertThat(cuenta("2027-01", "100").getFechaVencimiento()).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(cuenta("2028-01", "100").getFechaVencimiento()).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    @DisplayName("Sin pagos queda Pendiente de Pago hasta el día en que vence, y En Mora desde el siguiente")
    void sinPagos() {
        CuentaMensual cuenta = cuenta("2026-10", "1000");

        assertThat(cuenta.estadoAl(VENCE)).isEqualTo(EstadoCuenta.PENDIENTE_PAGO);
        assertThat(cuenta.estadoAl(VENCE.plusDays(1))).isEqualTo(EstadoCuenta.EN_MORA);
    }

    @Test
    @DisplayName("Un pago parcial la deja en Pago Parcial, pero vencida sigue En Mora; el del total la cancela aunque esté vencida")
    void pagos() {
        CuentaMensual cuenta = cuenta("2026-10", "1000");

        cuenta.registrarPago(new BigDecimal("400"), VENCE);
        assertThat(cuenta.getTotalPagado()).isEqualByComparingTo("400.00");
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("600.00");
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.PAGO_PARCIAL);
        assertThat(cuenta.estadoAl(VENCE.plusDays(1))).isEqualTo(EstadoCuenta.EN_MORA);

        cuenta.registrarPago(new BigDecimal("600"), VENCE.plusMonths(2));
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("0.00");
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.COMPLETAMENTE_CANCELADA);
        assertThat(cuenta.estadoAl(VENCE.plusYears(1))).isEqualTo(EstadoCuenta.COMPLETAMENTE_CANCELADA);
    }

    @Test
    @DisplayName("Anular un pago devuelve su monto al saldo y el estado se recalcula sin él")
    void anulacion() {
        CuentaMensual cuenta = cuenta("2026-10", "1000");
        cuenta.registrarPago(new BigDecimal("1000"), VENCE);

        cuenta.anularPago(new BigDecimal("1000"), VENCE);
        assertThat(cuenta.getTotalPagado()).isEqualByComparingTo("0.00");
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("1000.00");
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.PENDIENTE_PAGO);
        assertThat(cuenta.tienePagos()).isFalse();
    }

    @Test
    @DisplayName("Un cargo nuevo después de cancelada sube el total: el saldo crece y vuelve a Pago Parcial")
    void cargoNuevoDespuesDeCancelada() {
        CuentaMensual cuenta = cuenta("2026-10", "1000");
        cuenta.registrarPago(new BigDecimal("1000"), VENCE);

        cuenta.setTotalAPagar(new BigDecimal("1205.00"));
        cuenta.actualizarSaldo(VENCE);
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("205.00");
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.PAGO_PARCIAL);
    }

    private static CuentaMensual cuenta(String periodo, String total) {
        CuentaMensual cuenta = new CuentaMensual();
        cuenta.setPeriodo(periodo);
        cuenta.setTotalAPagar(new BigDecimal(total).setScale(2));
        cuenta.actualizarSaldo(LocalDate.parse(periodo + "-01"));
        return cuenta;
    }
}
