package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;

/**
 * YS-169. Una especialidad con su tarifa de consulta, para la Fundación.
 *
 * <p>
 * Aparte de {@link EspecialidadResponse} a propósito: esa la usa también el
 * Médico General para referir, y la tarifa es un dato financiero que no
 * necesita.
 */
public record EspecialidadTarifaResponse(Long id, String nombre, BigDecimal tarifaConsulta) {
}
