package app.cabezadealgodon.com.gestionasilobackend.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import app.cabezadealgodon.com.gestionasilobackend.exception.TransicionInvalidaException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-108. La receta que el especialista emite durante la visita (HU-10,
 * RF-10): los medicamentos con su dosis, cantidad y forma de aplicación. Es
 * lo que la farmacia despacha (HU-15 a HU-17).
 *
 * <p>
 * Una por visita, como en el diseño (sección 3.4): si hacen falta más
 * medicamentos, se agregan a la misma receta. Es un dato clínico (RNF-03).
 *
 * <p>
 * {@code version} activa el bloqueo optimista de JPA. Lo pide el diseño para
 * HU-16: si dos farmacéuticos despachan la misma receta a la vez, o el
 * especialista la corrige mientras la farmacia la despacha, la segunda
 * escritura no pisa a la primera. {@code ManejadorGlobalExcepciones} responde
 * 409 a ese choque.
 */
@Entity
@Table(name = "receta", indexes = {
        // La bandeja de la farmacia (HU-15) filtra por estado.
        @Index(name = "idx_receta_estado", columnList = "estado")
})
@Getter
@Setter
@NoArgsConstructor
public class Receta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "visita_id", nullable = false, unique = true)
    private VisitaMedica visita;

    /** Lo que vale para toda la receta: "Tomar con alimentos", "Suspender si…". */
    @Column(name = "indicaciones_generales", length = 1000)
    private String indicacionesGenerales;

    /**
     * YS-126. Sin setter público, como en la orden: el estado solo cambia por
     * {@link #cambiarEstado}, que es donde se respeta el diagrama DE-03.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Setter(AccessLevel.NONE)
    private EstadoReceta estado = EstadoReceta.PENDIENTE_DESPACHO;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    /**
     * HU-16. La del último despacho en que se entregó algo: la de la entrega
     * completa, cuando la receta queda entregada. Vacía mientras la farmacia
     * no entregue nada. Cada medicamento guarda además la suya.
     */
    @Column(name = "fecha_despacho")
    private LocalDateTime fechaDespacho;

    /**
     * YS-126. Quien de la farmacia hizo el último despacho (la relación
     * "farmaceutico" del diseño). Vacío mientras nadie la despacha.
     */
    @ManyToOne
    @JoinColumn(name = "farmaceutico_id")
    private Usuario farmaceutico;

    @Version
    private Long version;

    /**
     * En el orden en que el especialista los escribió.
     *
     * <p>
     * YS-123. {@code @BatchSize}: la bandeja de la farmacia (HU-15) es una
     * página de recetas, y sin esto cada una pediría sus medicamentos en una
     * consulta aparte. Así, los de toda la página llegan en una sola.
     */
    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    @BatchSize(size = 50)
    private List<DetalleReceta> medicamentos = new ArrayList<>();

    public void agregarMedicamento(DetalleReceta detalle) {
        detalle.setReceta(this);
        medicamentos.add(detalle);
    }

    /**
     * YS-126. Lleva la receta a {@code nuevo} si DE-03 lo permite desde el
     * estado actual; si no, es un conflicto con su estado (409).
     */
    public void cambiarEstado(EstadoReceta nuevo) {
        if (!estado.puedeCambiarA(nuevo)) {
            throw new TransicionInvalidaException("La receta está en «" + estado.etiqueta()
                    + "» y no puede pasar a «" + nuevo.etiqueta() + "»");
        }
        estado = nuevo;
    }
}
