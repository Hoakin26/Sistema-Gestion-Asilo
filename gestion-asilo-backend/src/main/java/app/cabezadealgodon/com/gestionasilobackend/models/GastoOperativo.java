package app.cabezadealgodon.com.gestionasilobackend.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-148. Un gasto operativo del asilo (HU-23, RF-16), la entidad
 * {@code GastoOperativo} del diseño: el tipo, el monto, la fecha y, si hace
 * falta, una descripción. Aparte de {@code Donacion} y no como una
 * "transacción financiera" genérica, porque sus campos son otros (diseño, 3.4).
 *
 * <p>
 * Como las donaciones (YS-145), un gasto no se borra ni se edita: si se
 * registró mal, se anula con su motivo y queda en el histórico; uno anulado no
 * cuenta en los totales.
 */
@Entity
@Table(name = "gasto_operativo", indexes = {
        // El histórico y los reportes financieros van por fecha.
        @Index(name = "idx_gasto_fecha", columnList = "fecha")
})
@Getter
@Setter
@NoArgsConstructor
public class GastoOperativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_gasto", nullable = false, length = 30)
    private TipoGasto tipoGasto;

    /** Obligatoria con {@link TipoGasto#OTRO}; con los demás, opcional (el recibo de octubre, el proveedor). */
    @Column(length = 200)
    private String descripcion;

    /** {@code numeric(12,2)}, como pide el diseño para los montos. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /** El día en que se pagó; puede ser anterior a cuando se registró. */
    @Column(nullable = false)
    private LocalDate fecha;

    @ManyToOne(optional = false)
    @JoinColumn(name = "registrado_por_id", nullable = false)
    private Usuario registradoPor;

    @CreationTimestamp
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    /** Si se anuló. Sin setter: solo cambia por {@link #anular}. */
    @Column(nullable = false)
    @ColumnDefault("false")
    @Setter(AccessLevel.NONE)
    private boolean anulado = false;

    @Column(name = "motivo_anulacion", length = 300)
    @Setter(AccessLevel.NONE)
    private String motivoAnulacion;

    @ManyToOne
    @JoinColumn(name = "anulado_por_id")
    @Setter(AccessLevel.NONE)
    private Usuario anuladoPor;

    @Column(name = "fecha_anulacion")
    @Setter(AccessLevel.NONE)
    private LocalDateTime fechaAnulacion;

    /** Lo anula; el servicio ya comprobó que no lo estuviera. */
    public void anular(String motivo, Usuario usuario, LocalDateTime momento) {
        anulado = true;
        motivoAnulacion = motivo;
        anuladoPor = usuario;
        fechaAnulacion = momento;
    }
}
