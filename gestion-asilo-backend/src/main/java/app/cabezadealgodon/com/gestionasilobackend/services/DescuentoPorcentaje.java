package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import app.cabezadealgodon.com.gestionasilobackend.models.TipoDescuento;

/**
 * YS-136. Un porcentaje del costo: el 20 % de Q350.50 es Q70.10. Se redondea
 * al centavo con la regla de siempre (la mitad hacia arriba), una sola vez y
 * al final, para que la suma de los cargos no arrastre restos.
 */
@Component
public class DescuentoPorcentaje implements ReglaDescuento {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    @Override
    public TipoDescuento tipo() {
        return TipoDescuento.PORCENTAJE;
    }

    @Override
    public BigDecimal aplicar(BigDecimal monto, BigDecimal valor) {
        return monto.multiply(valor).divide(CIEN, 2, RoundingMode.HALF_UP);
    }
}
