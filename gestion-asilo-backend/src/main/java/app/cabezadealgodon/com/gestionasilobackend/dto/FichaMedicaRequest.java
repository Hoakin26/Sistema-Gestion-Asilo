package app.cabezadealgodon.com.gestionasilobackend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

/**
 * YS-58. Los datos clínicos de un interno. Solo los escribe el Médico General.
 *
 * <p>
 * Las listas reemplazan por completo a las guardadas: se manda la ficha como
 * debe quedar, no las diferencias. Es más simple de usar desde el formulario y
 * evita tener que inventar operaciones de "agregar" y "quitar" para cada una.
 */
public record FichaMedicaRequest(
        @Size(max = 5, message = "El tipo de sangre no puede exceder 5 caracteres")
        String tipoSangre,

        @Size(max = 500, message = "Las alergias no pueden exceder 500 caracteres")
        String alergias,

        @Size(max = 500, message = "El motivo de ingreso no puede exceder 500 caracteres")
        String motivoIngreso,

        // Pueden venir vacías: un interno puede no tener ningún padecimiento
        // registrado ni tomar medicamentos de forma permanente.
        @Valid List<PsicopatologiaRequest> psicopatologias,

        @Valid List<MedicamentoHabitualRequest> medicamentosHabituales) {
}
