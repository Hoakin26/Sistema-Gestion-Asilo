package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.time.LocalDate;

/** YS-58. Dato clínico: solo sale por los endpoints de ficha médica. */
public record PsicopatologiaResponse(
        Long id,
        String nombre,
        LocalDate fechaDiagnostico,
        String observaciones) {
}
