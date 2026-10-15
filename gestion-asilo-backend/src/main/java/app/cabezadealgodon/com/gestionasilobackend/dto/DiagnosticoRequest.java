package app.cabezadealgodon.com.gestionasilobackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * YS-105. El diagnóstico y las observaciones de la consulta (HU-09). El
 * diagnóstico es obligatorio; las observaciones, no.
 */
public record DiagnosticoRequest(
        @NotBlank(message = "El diagnóstico es obligatorio")
        @Size(max = 2000, message = "El diagnóstico no puede exceder 2000 caracteres")
        String diagnostico,

        @Size(max = 2000, message = "Las observaciones no pueden exceder 2000 caracteres")
        String observaciones) {
}
