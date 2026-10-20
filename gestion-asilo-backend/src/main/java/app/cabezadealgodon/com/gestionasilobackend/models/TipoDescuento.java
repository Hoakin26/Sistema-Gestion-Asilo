package app.cabezadealgodon.com.gestionasilobackend.models;

/**
 * YS-136. Cómo descuenta la fundación un servicio (HU-19): un porcentaje del
 * costo o un monto fijo por cargo. Cada uno tiene su regla de cálculo
 * ({@code ReglaDescuento}); agregar otro tipo es agregar su constante y su
 * regla, sin tocar a quien calcula (principio abierto/cerrado).
 */
public enum TipoDescuento {

    /** Un porcentaje del costo original, de 0 a 100. */
    PORCENTAJE,

    /** Un monto en quetzales que se resta de cada cargo. */
    MONTO_FIJO
}
