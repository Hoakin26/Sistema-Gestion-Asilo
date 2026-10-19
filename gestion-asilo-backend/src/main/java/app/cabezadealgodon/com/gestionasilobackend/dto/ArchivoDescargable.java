package app.cabezadealgodon.com.gestionasilobackend.dto;

import org.springframework.core.io.Resource;

/**
 * YS-117. Un archivo listo para descargarse: el contenido y lo que el
 * controlador necesita para las cabeceras de la respuesta.
 */
public record ArchivoDescargable(Resource contenido, String nombre, String tipo, long tamano) {
}
