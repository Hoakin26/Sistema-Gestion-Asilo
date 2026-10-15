package app.cabezadealgodon.com.gestionasilobackend.dto;

/** YS-58. Dato clínico: solo sale por los endpoints de ficha médica. */
public record MedicamentoHabitualResponse(
        Long id,
        String nombre,
        String dosis,
        String frecuencia) {
}
