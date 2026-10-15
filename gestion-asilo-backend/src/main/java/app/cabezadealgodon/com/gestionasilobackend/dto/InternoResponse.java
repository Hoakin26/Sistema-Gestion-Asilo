package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * YS-58. El expediente administrativo de un interno.
 *
 * <p>
 * <b>Sin un solo dato clínico, y eso es el RNF-03.</b> Lo devuelven también el
 * Administrador del Asilo y enfermería, así que aquí no puede aparecer nada de
 * la ficha médica. No hace falta recordar excluirlo: {@code Interno} ni
 * siquiera tiene camino hacia {@code FichaMedica} (YS-57), de modo que no hay
 * un campo que se pueda agregar por descuido.
 */
public record InternoResponse(
        Long id,
        String nombreCompleto,
        String dpi,
        LocalDate fechaNacimiento,
        LocalDate fechaIngreso,
        BigDecimal cuotaMensual,
        boolean activo,
        FamiliarResponse familiarResponsable,
        LocalDateTime fechaCreacion) {
}
