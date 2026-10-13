package app.cabezadealgodon.com.gestionasilobackend.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-105. Lo que se le cobra al familiar por un servicio dado al interno
 * (RF-12): un cargo por servicio, con el monto original, el descuento de la
 * fundación y el neto, y la visita de la que salió (sección 3.4 del diseño).
 *
 * <p>
 * Lo crea HU-09 al finalizar la visita, con el cargo de la consulta. HU-14 y
 * HU-17 agregan los de laboratorio y farmacia; HU-18 los consolida en la
 * cuenta mensual, y HU-19 calcula el descuento.
 *
 * <p>
 * Los montos se copian al crearlo y no se recalculan: si cambia la tarifa o la
 * política de descuento, el cargo conserva lo que se cobró.
 */
@Entity
@Table(name = "cargo_servicio", indexes = {
        // La cuenta mensual (HU-18) suma por interno y por fecha.
        @Index(name = "idx_cargo_interno_fecha", columnList = "interno_id, fecha"),
        @Index(name = "idx_cargo_visita", columnList = "visita_id")
})
@Getter
@Setter
@NoArgsConstructor
public class CargoServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoServicio tipo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "interno_id", nullable = false)
    private Interno interno;

    /** La visita de la que salió el servicio. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "visita_id", nullable = false)
    private VisitaMedica visita;

    @Column(name = "monto_original", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoOriginal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal descuento;

    /** Monto original menos descuento, nunca menor que cero (regla de HU-19). */
    @Column(name = "monto_neto", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoNeto;

    /** El día del servicio: decide en qué cuenta mensual cae. */
    @Column(nullable = false)
    private LocalDate fecha;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
