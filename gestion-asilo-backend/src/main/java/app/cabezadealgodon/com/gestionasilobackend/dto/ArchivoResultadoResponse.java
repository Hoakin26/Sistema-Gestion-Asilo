package app.cabezadealgodon.com.gestionasilobackend.dto;

/**
 * YS-117. El archivo adjunto a un resultado de laboratorio, sin su contenido:
 * lo que hace falta para mostrarlo y ofrecer la descarga. {@code tipo} es el
 * tipo MIME ({@code application/pdf}, {@code image/jpeg} o {@code image/png});
 * {@code tamano}, en bytes.
 */
public record ArchivoResultadoResponse(String nombre, String tipo, long tamano) {
}
