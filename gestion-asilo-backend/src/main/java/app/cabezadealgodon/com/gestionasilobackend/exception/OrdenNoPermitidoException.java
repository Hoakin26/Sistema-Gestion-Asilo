package app.cabezadealgodon.com.gestionasilobackend.exception;

import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * YS-166. Se pidió ordenar una lista por un campo que no está permitido.
 *
 * <p>
 * Las listas con consulta propia ({@code @Query}) no avisan de un campo
 * inexistente con un error de Spring Data sino de Hibernate, y salía como un
 * 500: un error del servidor por algo que escribió mal el cliente. En vez de
 * atrapar esa excepción, que también podría venir de un error real, cada
 * controlador declara por qué campos se puede ordenar y rechaza el resto
 * antes de consultar. Se responde 400.
 */
public class OrdenNoPermitidoException extends RuntimeException {

    public OrdenNoPermitidoException(String propiedad) {
        super("No se puede ordenar por '" + propiedad + "'");
    }

    /** Rechaza el paginado si ordena por algo fuera de {@code permitidos}. */
    public static Pageable validar(Pageable paginado, Set<String> permitidos) {
        for (Sort.Order orden : paginado.getSort()) {
            if (!permitidos.contains(orden.getProperty())) {
                throw new OrdenNoPermitidoException(orden.getProperty());
            }
        }
        return paginado;
    }
}
