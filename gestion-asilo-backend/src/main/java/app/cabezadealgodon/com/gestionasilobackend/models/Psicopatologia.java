package app.cabezadealgodon.com.gestionasilobackend.models;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-57. Un padecimiento psicológico o psiquiátrico diagnosticado al interno.
 * Dato clínico: cuelga de la ficha médica y por lo tanto queda fuera del
 * alcance del Administrador del Asilo (RNF-03).
 */
@Entity
@Table(name = "psicopatologia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Psicopatologia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ficha_medica_id", nullable = false)
    private FichaMedica fichaMedica;

    @Column(nullable = false, length = 150)
    private String nombre;

    /** Opcional: no siempre se conoce cuándo se diagnosticó. */
    @Column(name = "fecha_diagnostico")
    private LocalDate fechaDiagnostico;

    @Column(length = 500)
    private String observaciones;
}
