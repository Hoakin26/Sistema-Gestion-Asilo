package app.cabezadealgodon.com.gestionasilobackend.models;

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
 * YS-57. Un medicamento que el interno toma de forma permanente, lo que el
 * documento de análisis llamaba "medicamentos de cajón". No confundir con los
 * de una receta: estos no tienen despacho ni se agotan, describen su
 * tratamiento de base.
 *
 * <p>
 * Dato clínico: cuelga de la ficha médica y queda fuera del alcance del
 * Administrador del Asilo (RNF-03).
 */
@Entity
@Table(name = "medicamento_habitual")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MedicamentoHabitual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ficha_medica_id", nullable = false)
    private FichaMedica fichaMedica;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String dosis;

    @Column(nullable = false, length = 100)
    private String frecuencia;
}
