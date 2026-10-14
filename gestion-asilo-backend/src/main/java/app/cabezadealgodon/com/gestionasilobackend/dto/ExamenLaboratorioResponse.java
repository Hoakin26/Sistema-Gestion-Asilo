package app.cabezadealgodon.com.gestionasilobackend.dto;

/**
 * YS-111. Un examen del catálogo, como lo elige el especialista. Sin el costo
 * de referencia: es un dato del asilo, no de quien ordena el examen.
 */
public record ExamenLaboratorioResponse(Long id, String nombre) {
}
