package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;

import app.cabezadealgodon.com.gestionasilobackend.models.TipoDescuento;

/**
 * YS-136. Cómo se calcula un tipo de descuento (patrón Estrategia del diseño,
 * 4.7). Cada implementación es un componente de Spring y
 * {@link CalculadoraDescuento} las recibe todas: un tipo nuevo es una clase
 * nueva, y la calculadora no cambia (principio abierto/cerrado).
 */
public interface ReglaDescuento {

    /** El tipo que calcula esta regla. */
    TipoDescuento tipo();

    /**
     * El descuento sobre {@code monto} con {@code valor} (un porcentaje o un
     * monto, según la regla). Puede salir mayor que el monto: quien llama lo
     * limita, para que el neto nunca sea negativo.
     */
    BigDecimal aplicar(BigDecimal monto, BigDecimal valor);
}
