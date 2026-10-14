package app.cabezadealgodon.com.gestionasilobackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** YS-111. Un examen del catálogo y lo que el laboratorio tiene que saber de él. */
public record ExamenOrdenadoRequest(
        @NotNull(message = "Seleccione el examen")
        Long examenId,

        @Size(max = 500, message = "Las indicaciones no pueden exceder 500 caracteres")
        String indicaciones) {
}
