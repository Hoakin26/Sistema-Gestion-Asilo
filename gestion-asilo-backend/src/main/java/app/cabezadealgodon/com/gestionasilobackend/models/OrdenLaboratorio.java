package app.cabezadealgodon.com.gestionasilobackend.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import app.cabezadealgodon.com.gestionasilobackend.exception.TransicionInvalidaException;
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
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-111. Un examen que el especialista ordena durante la visita (HU-11,
 * RF-08): una orden por examen, como en el diseño, para que el laboratorio
 * procese y cobre cada uno por separado (HU-12 a HU-14).
 *
 * <p>
 * Es un dato clínico (RNF-03). {@code version} activa el bloqueo optimista
 * que el diseño pide para el laboratorio, igual que en la receta.
 */
@Entity
@Table(name = "orden_laboratorio", indexes = {
        // La bandeja del laboratorio (HU-12) filtra por estado.
        @Index(name = "idx_orden_estado", columnList = "estado"),
        @Index(name = "idx_orden_visita", columnList = "visita_id")
})
@Getter
@Setter
@NoArgsConstructor
public class OrdenLaboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "visita_id", nullable = false)
    private VisitaMedica visita;

    @ManyToOne(optional = false)
    @JoinColumn(name = "examen_id", nullable = false)
    private ExamenLaboratorio examen;

    /** Lo que el laboratorio necesita saber del examen: "en ayunas", "muestra de la mañana". */
    @Column(length = 500)
    private String indicaciones;

    /**
     * YS-114. Sin setter público, como en la solicitud: el estado solo cambia
     * por {@link #cambiarEstado}, que es donde se respeta el diagrama DE-02.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Setter(AccessLevel.NONE)
    private EstadoOrden estado = EstadoOrden.PENDIENTE;

    /**
     * YS-114. Quien del laboratorio tomó la orden (la relación "laboratorista"
     * del diseño). Vacío mientras está pendiente.
     */
    @ManyToOne
    @JoinColumn(name = "laboratorista_id")
    private Usuario laboratorista;

    /**
     * RF-08: el costo para el asilo, copiado del catálogo al ordenar. Si el
     * catálogo cambia después, la orden conserva el que tenía. El laboratorio
     * registra el costo real del examen realizado en HU-14.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal costo;

    @Column(name = "fecha_orden", nullable = false)
    private LocalDateTime fechaOrden;

    @Version
    private Long version;

    /** YS-114. Lleva la orden a otro estado, si el diagrama DE-02 lo permite. */
    public void cambiarEstado(EstadoOrden nuevo) {
        if (!estado.puedeCambiarA(nuevo)) {
            throw new TransicionInvalidaException("La orden de «" + examen.getNombre() + "» está en «"
                    + estado.etiqueta() + "» y no puede pasar a «" + nuevo.etiqueta() + "»");
        }
        estado = nuevo;
    }
}
