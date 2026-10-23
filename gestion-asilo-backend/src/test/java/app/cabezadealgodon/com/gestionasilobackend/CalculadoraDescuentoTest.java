package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import app.cabezadealgodon.com.gestionasilobackend.models.PoliticaDescuento;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoDescuento;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;
import app.cabezadealgodon.com.gestionasilobackend.services.CalculadoraDescuento;
import app.cabezadealgodon.com.gestionasilobackend.services.DescuentoMontoFijo;
import app.cabezadealgodon.com.gestionasilobackend.services.DescuentoPorcentaje;
import app.cabezadealgodon.com.gestionasilobackend.services.DesgloseCosto;

/**
 * YS-138. El cálculo del descuento de la fundación (HU-19), sin base ni
 * Spring: las dos reglas, el redondeo, el neto que nunca es negativo y qué
 * política rige según la fecha del servicio.
 */
@DisplayName("Cálculo del descuento de la fundación (YS-138)")
class CalculadoraDescuentoTest {

    private static final LocalDate OCTUBRE = LocalDate.of(2026, 10, 20);

    /** Las políticas se pasan ya leídas, así que no hace falta el repositorio. */
    private final CalculadoraDescuento calculadora = new CalculadoraDescuento(null,
            List.of(new DescuentoPorcentaje(), new DescuentoMontoFijo()));

    @Test
    @DisplayName("Un porcentaje se calcula sobre el costo y se redondea al centavo")
    void porcentaje() {
        List<PoliticaDescuento> politicas = List.of(politica(TipoServicio.CONSULTA, TipoDescuento.PORCENTAJE, "20",
                LocalDate.of(2026, 10, 1)));

        assertThat(calcular(TipoServicio.CONSULTA, "350.50", politicas)).isEqualTo(desglose("350.50", "70.10", "280.40"));
        assertThat(calcular(TipoServicio.CONSULTA, "0.10", List.of(politica(TipoServicio.CONSULTA,
                TipoDescuento.PORCENTAJE, "12.5", LocalDate.of(2026, 10, 1))))).isEqualTo(desglose("0.10", "0.01", "0.09"));
        assertThat(calcular(TipoServicio.CONSULTA, "100", List.of(politica(TipoServicio.CONSULTA,
                TipoDescuento.PORCENTAJE, "100", LocalDate.of(2026, 10, 1))))).isEqualTo(desglose("100.00", "100.00", "0.00"));
    }

    @Test
    @DisplayName("Un monto fijo se resta del cargo, y si pasa del costo el neto queda en cero, no negativo")
    void montoFijo() {
        List<PoliticaDescuento> politicas = List.of(politica(TipoServicio.FARMACIA, TipoDescuento.MONTO_FIJO, "50",
                LocalDate.of(2026, 10, 1)));

        assertThat(calcular(TipoServicio.FARMACIA, "120", politicas)).isEqualTo(desglose("120.00", "50.00", "70.00"));
        assertThat(calcular(TipoServicio.FARMACIA, "35.50", politicas)).isEqualTo(desglose("35.50", "35.50", "0.00"));
    }

    @Test
    @DisplayName("Sin política, o con una de cero, no hay descuento")
    void sinDescuento() {
        assertThat(calcular(TipoServicio.LABORATORIO, "75", List.of())).isEqualTo(desglose("75.00", "0.00", "75.00"));
        assertThat(calcular(TipoServicio.LABORATORIO, "75", List.of(politica(TipoServicio.LABORATORIO,
                TipoDescuento.PORCENTAJE, "0", LocalDate.of(2026, 1, 1))))).isEqualTo(desglose("75.00", "0.00", "75.00"));
    }

    /** Decidido el 20/10: a cada cargo le toca la política vigente el día del servicio. */
    @Test
    @DisplayName("Rige la política más reciente que ya había empezado en la fecha del servicio, y solo la de su servicio")
    void vigenciaPorFecha() {
        List<PoliticaDescuento> politicas = List.of(
                politica(TipoServicio.CONSULTA, TipoDescuento.PORCENTAJE, "20", LocalDate.of(2026, 9, 1)),
                politica(TipoServicio.CONSULTA, TipoDescuento.PORCENTAJE, "10", LocalDate.of(2026, 10, 15)),
                politica(TipoServicio.CONSULTA, TipoDescuento.PORCENTAJE, "50", LocalDate.of(2026, 12, 1)));
        BigDecimal cien = new BigDecimal("100");

        assertThat(calculadora.calcular(TipoServicio.CONSULTA, cien, LocalDate.of(2026, 8, 31), politicas).descuento())
                .isEqualByComparingTo("0");
        assertThat(calculadora.calcular(TipoServicio.CONSULTA, cien, LocalDate.of(2026, 9, 1), politicas).descuento())
                .isEqualByComparingTo("20");
        assertThat(calculadora.calcular(TipoServicio.CONSULTA, cien, LocalDate.of(2026, 10, 14), politicas).descuento())
                .isEqualByComparingTo("20");
        assertThat(calculadora.calcular(TipoServicio.CONSULTA, cien, LocalDate.of(2026, 10, 15), politicas).descuento())
                .isEqualByComparingTo("10");
        // La de diciembre todavía no empieza.
        assertThat(calculadora.calcular(TipoServicio.CONSULTA, cien, OCTUBRE, politicas).descuento())
                .isEqualByComparingTo("10");
        // Las de la consulta no tocan al laboratorio.
        assertThat(calculadora.calcular(TipoServicio.LABORATORIO, cien, OCTUBRE, politicas).descuento())
                .isEqualByComparingTo("0");
    }

    private DesgloseCosto calcular(TipoServicio tipo, String monto, List<PoliticaDescuento> politicas) {
        return calculadora.calcular(tipo, new BigDecimal(monto), OCTUBRE, politicas);
    }

    private static DesgloseCosto desglose(String original, String descuento, String neto) {
        return new DesgloseCosto(new BigDecimal(original), new BigDecimal(descuento), new BigDecimal(neto));
    }

    private static PoliticaDescuento politica(TipoServicio servicio, TipoDescuento tipo, String valor,
            LocalDate desde) {
        PoliticaDescuento politica = new PoliticaDescuento();
        politica.setTipoServicio(servicio);
        politica.setTipoDescuento(tipo);
        politica.setValor(new BigDecimal(valor));
        politica.setVigenteDesde(desde);
        return politica;
    }
}
