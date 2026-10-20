package app.cabezadealgodon.com.gestionasilobackend.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-108. Un medicamento de la receta, con lo que pide HU-10: nombre, dosis,
 * cantidad y tiempo o forma de aplicación.
 *
 * <p>
 * El diseño llama {@code duracion} al último campo; aquí es
 * {@code formaAplicacion}, con las palabras de la historia, porque cubre las
 * dos cosas: "cada 8 horas por 7 días, vía oral". HU-16 y HU-17 agregan lo
 * que es de la farmacia: si se entregó y cuánto costó. YS-126: si se entregó
 * y cuándo.
 */
@Entity
@Table(name = "detalle_receta")
@Getter
@Setter
@NoArgsConstructor
public class DetalleReceta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "receta_id", nullable = false)
    private Receta receta;

    @Column(nullable = false, length = 150)
    private String medicamento;

    /** "500 mg", "1 tableta": lo que se toma cada vez. */
    @Column(nullable = false, length = 100)
    private String dosis;

    /** Cuántas unidades despacha la farmacia. */
    @Column(nullable = false)
    private int cantidad;

    @Column(name = "forma_aplicacion", nullable = false, length = 200)
    private String formaAplicacion;

    /**
     * YS-126. Si la farmacia ya lo entregó (la "marca de entregado" del
     * diseño). Sin marcar, está pendiente: todavía no se despacha, o se
     * despachó sin él por falta de existencias (CU-005, E.1).
     *
     * <p>
     * El valor por defecto va en la columna y no solo en Java, como en
     * {@code Usuario.debeCambiarPassword}: la tabla ya tiene filas, y
     * PostgreSQL rechaza agregar una columna NOT NULL sin él. Las recetas que
     * ya existen nacen sin entregar, que es lo que son.
     */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    @Setter(AccessLevel.NONE)
    private boolean entregado = false;

    /** YS-126. Cuándo se entregó; vacía mientras está pendiente. */
    @Column(name = "fecha_entrega")
    @Setter(AccessLevel.NONE)
    private LocalDateTime fechaEntrega;

    /** YS-126. Lo marca entregado; el despacho ya comprobó que no lo estuviera. */
    public void entregar(LocalDateTime momento) {
        entregado = true;
        fechaEntrega = momento;
    }
}
