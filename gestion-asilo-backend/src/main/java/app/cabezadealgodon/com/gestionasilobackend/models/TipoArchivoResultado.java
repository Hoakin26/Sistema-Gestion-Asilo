package app.cabezadealgodon.com.gestionasilobackend.models;

import java.util.Arrays;
import java.util.Optional;

/**
 * YS-117. Los tipos de archivo que acepta un resultado de laboratorio (HU-13):
 * el informe en PDF y la foto del resultado impreso. Decidido el 19/10.
 *
 * <p>
 * El tipo se reconoce por los primeros bytes del archivo, no por la extensión
 * ni por lo que declara el navegador: los dos los escribe quien sube el
 * archivo, y un ejecutable renombrado a {@code .pdf} los pasaría.
 */
public enum TipoArchivoResultado {

    PDF("application/pdf", "pdf", new byte[] { '%', 'P', 'D', 'F', '-' }),
    PNG("image/png", "png", new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A }),
    JPEG("image/jpeg", "jpg", new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });

    private final String tipoMime;
    private final String extension;
    private final byte[] firma;

    TipoArchivoResultado(String tipoMime, String extension, byte[] firma) {
        this.tipoMime = tipoMime;
        this.extension = extension;
        this.firma = firma;
    }

    public String tipoMime() {
        return tipoMime;
    }

    public String extension() {
        return extension;
    }

    /** El tipo cuyo comienzo coincide con el del contenido, si alguno coincide. */
    public static Optional<TipoArchivoResultado> detectar(byte[] contenido) {
        return Arrays.stream(values()).filter(tipo -> tipo.empiezaCon(contenido)).findFirst();
    }

    private boolean empiezaCon(byte[] contenido) {
        if (contenido.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if (contenido[i] != firma[i]) {
                return false;
            }
        }
        return true;
    }
}
