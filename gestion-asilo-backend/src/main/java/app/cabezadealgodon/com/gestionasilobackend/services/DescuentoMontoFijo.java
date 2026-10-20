package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import app.cabezadealgodon.com.gestionasilobackend.models.TipoDescuento;

/**
 * YS-136. Un monto fijo por cargo: Q50 menos en cada consulta, en cada examen
 * o en cada medicamento entregado, según el servicio. Puede pasar del costo;
 * {@link CalculadoraDescuento} lo limita para que el neto quede en cero.
 */
@Component
public class DescuentoMontoFijo implements ReglaDescuento {

    @Override
    public TipoDescuento tipo() {
        return TipoDescuento.MONTO_FIJO;
    }

    @Override
    public BigDecimal aplicar(BigDecimal monto, BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
