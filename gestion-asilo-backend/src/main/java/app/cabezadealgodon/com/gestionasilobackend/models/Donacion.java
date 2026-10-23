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
 * YS-145. Una donación monetaria al asilo (HU-22, RF-15), la entidad
 * {@code Donacion} del diseño: el monto, la fecha, el donante y su tipo. Es
 * una entidad aparte de {@code GastoOperativo} y no una "transacción
 * financiera" genérica, porque sus campos son otros (diseño, 3.4).
 *
 * <p>
 * Como los pagos (YS-176), una donación no se borra ni se edita: es un ingreso
 * registrado, y el reporte de entradas (RF-20b) la suma. Si se registró mal, se
 * anula con su motivo y queda en el histórico; una anulada no cuenta en los
 * totales.
 */
@Entity
@Table(name = "donacion", indexes = {
        // El histórico y el reporte de entradas (HU-27) van por fecha.
        @Index(name = "idx_donacion_fecha", columnList = "fecha")
})
@Getter
@Setter
@NoArgsConstructor
public class Donacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** {@code numeric(12,2)}, como pide el diseño para los montos: una donación puede ser grande. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /** El día en que se recibió; puede ser anterior a cuando se registró. */
    @Column(nullable = false)
    private LocalDate fecha;

    /** El nombre de la persona, la empresa o la institución que dona. */
    @Column(nullable = false, length = 150)
    private String donante;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_donante", nullable = false, length = 30)
    private TipoDonante tipoDonante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "registrada_por_id", nullable = false)
    private Usuario registradaPor;

    @CreationTimestamp
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    /** Si se anuló. Sin setter: solo cambia por {@link #anular}. */
    @Column(nullable = false)
    @ColumnDefault("false")
    @Setter(AccessLevel.NONE)
    private boolean anulada = false;

    @Column(name = "motivo_anulacion", length = 300)
    @Setter(AccessLevel.NONE)
    private String motivoAnulacion;

    @ManyToOne
    @JoinColumn(name = "anulada_por_id")
    @Setter(AccessLevel.NONE)
    private Usuario anuladaPor;

    @Column(name = "fecha_anulacion")
    @Setter(AccessLevel.NONE)
    private LocalDateTime fechaAnulacion;

    /** La anula; el servicio ya comprobó que no lo estuviera. */
    public void anular(String motivo, Usuario usuario, LocalDateTime momento) {
        anulada = true;
        motivoAnulacion = motivo;
        anuladaPor = usuario;
        fechaAnulacion = momento;
    }
}
