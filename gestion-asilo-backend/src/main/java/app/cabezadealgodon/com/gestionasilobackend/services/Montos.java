package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import app.cabezadealgodon.com.gestionasilobackend.exception.CampoInvalidoException;

/**
 * YS-129. Un costo como lo escribe una persona. Salió del costo del
 * laboratorio (YS-120) para que la farmacia (HU-17) aplique las mismas reglas y
 * los mismos mensajes: los dos terminan en la cuenta del familiar.
 */
final class Montos {

    /**
     * Hasta 8 enteros y 2 decimales, lo que cabe en las columnas de montos
     * (precisión 10, escala 2). Sin notación científica ni signos: "1e3" sería
     * un monto válido para {@code BigDecimal}, pero nadie lo escribe así por
     * querer decir 1000.
     */
    private static final Pattern MONTO = Pattern.compile("\\d{1,8}(\\.\\d{1,2})?");

    private Montos() {
    }

    /**
     * El costo de {@code texto}, mayor que cero y con dos decimales; la coma se
     * acepta como punto. Si no sirve, el error va en {@code campo}, y
     * {@code faltante} es el mensaje para cuando viene vacío.
     */
    static BigDecimal costo(String texto, String campo, String faltante) {
        String limpio = texto == null ? "" : texto.strip().replace(',', '.');
        if (limpio.isEmpty()) {
            throw new CampoInvalidoException(campo, faltante);
        }
        if (limpio.startsWith("-")) {
            throw new CampoInvalidoException(campo, "El costo tiene que ser mayor que cero");
        }
        if (!MONTO.matcher(limpio).matches()) {
            throw new CampoInvalidoException(campo, "Escriba un monto como 75 o 75.50");
        }
        BigDecimal monto = new BigDecimal(limpio).setScale(2);
        if (monto.signum() == 0) {
            throw new CampoInvalidoException(campo, "El costo tiene que ser mayor que cero");
        }
        return monto;
    }
}
