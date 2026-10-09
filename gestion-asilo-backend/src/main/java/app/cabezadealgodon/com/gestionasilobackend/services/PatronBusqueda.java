package app.cabezadealgodon.com.gestionasilobackend.services;

import java.text.Normalizer;
import java.util.Locale;

/**
 * YS-62 y YS-166. Cómo se convierte lo que escribe una persona en un patrón
 * {@code LIKE} para las búsquedas de las listas.
 *
 * <p>
 * Se compara sin mayúsculas ni tildes: en la tableta casi nadie escribe
 * "Núñez", escribe "nunez". Del lado de la base, las consultas pasan la
 * columna por {@code translate} y {@code lower}; de este lado, el término pasa
 * por aquí. Así los dos quedan en la misma forma.
 */
final class PatronBusqueda {

    private PatronBusqueda() {
    }

    /** Sin tildes ni mayúsculas: "Núñez" queda "nunez". */
    static String normalizado(String termino) {
        String limpio = termino == null ? "" : termino.trim();
        return Normalizer.normalize(limpio, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * El patrón de "contiene". Los comodines que escriba el usuario se escapan
     * para que cuenten como texto: buscar "%" no debe traer todo. Las
     * consultas declaran {@code escape '\'}.
     */
    static String contiene(String texto) {
        String escapado = texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escapado + "%";
    }
}
