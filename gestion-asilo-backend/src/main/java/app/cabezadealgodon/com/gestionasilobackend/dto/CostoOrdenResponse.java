package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;

/**
 * HU-14 (YS-120). El costo de una orden de laboratorio, solo para el
 * laboratorio: mientras la orden no tiene resultado es el sugerido (el costo
 * de referencia del catálogo, RF-08 v1.1); con resultado, el que registró el
 * laboratorio y se cargó a la cuenta. {@code registrado} dice cuál de los dos.
 *
 * <p>
 * Va aparte de {@code OrdenLaboratorioResponse}, que también recibe el
 * especialista: él ordena por criterio clínico y no ve costos (decidido el
 * 19/10, como en HU-12).
 */
public record CostoOrdenResponse(BigDecimal costo, boolean registrado) {
}
