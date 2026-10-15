package app.cabezadealgodon.com.gestionasilobackend.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-57. Los datos clínicos de un interno, separados de sus datos personales
 * por el RNF-03: el Administrador del Asilo nunca debe poder leerlos.
 *
 * <p>
 * Esta entidad posee la relación con {@link Interno} y no al revés. Así el
 * camino solo se puede recorrer en una dirección —de lo clínico a lo personal,
 * nunca de lo personal a lo clínico— y la vista administrativa no tiene campos
 * que ocultar porque no tiene forma de llegar hasta aquí.
 *
 * <p>
 * El historial médico no se guarda dentro de la ficha: se arma consultando las
 * visitas, órdenes y recetas del interno (HU-02). Guardarlo dos veces
 * permitiría que las dos versiones discreparan.
 */
@Entity
@Table(name = "ficha_medica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FichaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Único: un interno tiene una sola ficha médica. */
    @OneToOne(optional = false)
    @JoinColumn(name = "interno_id", nullable = false, unique = true)
    private Interno interno;

    @Column(name = "tipo_sangre", length = 5)
    private String tipoSangre;

    @Column(length = 500)
    private String alergias;

    @Column(name = "motivo_ingreso", length = 500)
    private String motivoIngreso;

    /*
     * Listas y no texto libre, como estaba en el diagrama de la Fase de
     * Análisis: HU-01 pide registrar varias psicopatologías y varios
     * medicamentos, y en un solo campo de texto no se pueden contar, filtrar
     * ni mostrar por separado.
     *
     * orphanRemoval: quitar un medicamento de la lista lo borra. Sin eso
     * quedaría huérfano en la base, apareciendo en consultas que ya no debería.
     */
    @OneToMany(mappedBy = "fichaMedica", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Psicopatologia> psicopatologias = new ArrayList<>();

    @OneToMany(mappedBy = "fichaMedica", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicamentoHabitual> medicamentosHabituales = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    /**
     * Mantienen los dos lados de la relación de acuerdo. Asignar solo la lista
     * dejaría la columna `ficha_medica_id` en nulo y el elemento no se
     * guardaría: es el error clásico de las relaciones bidireccionales.
     */
    public void agregarPsicopatologia(Psicopatologia psicopatologia) {
        psicopatologia.setFichaMedica(this);
        psicopatologias.add(psicopatologia);
    }

    public void agregarMedicamento(MedicamentoHabitual medicamento) {
        medicamento.setFichaMedica(this);
        medicamentosHabituales.add(medicamento);
    }
}
