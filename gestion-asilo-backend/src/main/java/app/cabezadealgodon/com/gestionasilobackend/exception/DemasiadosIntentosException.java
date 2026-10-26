package app.cabezadealgodon.com.gestionasilobackend.exception;

import java.time.Duration;

/**
 * DEF-01. Demasiados inicios de sesión fallidos con un correo o desde una
 * dirección. Sale como 429 (ManejadorGlobalExcepciones). No hereda de
 * AuthenticationException a propósito: esa responde 401.
 */
public class DemasiadosIntentosException extends RuntimeException {

    private final Duration espera;

    public DemasiadosIntentosException(Duration espera) {
        super("Demasiados intentos fallidos. Espere " + minutos(espera) + " e intente de nuevo");
        this.espera = espera;
    }

    public Duration getEspera() {
        return espera;
    }

    private static String minutos(Duration espera) {
        long minutos = Math.max(1, (espera.toSeconds() + 59) / 60);
        return minutos == 1 ? "1 minuto" : minutos + " minutos";
    }
}
