package app.cabezadealgodon.com.gestionasilobackend.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-136. Un descuento de la fundación para un tipo de servicio (HU-19,
 * RF-12): un porcentaje o un monto fijo, vigente desde una fecha. La define el
 * Administrador de la Fundación, porque el descuento lo da la fundación.
 *
 * <p>
 * Decidido el 20/10: el descuento va por la fecha del servicio. Las políticas
 * no se reemplazan, se versionan: cada una rige desde {@code vigenteDesde}
 * hasta que empieza la siguiente del mismo servicio, y a cada cargo le toca la
 * vigente el día en que se dio el servicio. Así una política "desde el 1 de
 * octubre" cubre también los cargos de octubre que ya existían, y los de
 * septiembre conservan la anterior. Una por servicio y fecha
 * ({@code uniqueConstraints}): volver a definir la misma fecha la corrige.
 *
 * <p>
 * "Sin descuento" es un porcentaje de cero, que también se versiona: quitar un
 * descuento desde una fecha no borra lo que rigió antes.
 */
@Entity
@Table(name = "politica_descuento", uniqueConstraints = {
        @UniqueConstraint(name = "uk_politica_servicio_fecha", columnNames = { "tipo_servicio", "vigente_desde" })
})
@Getter
@Setter
@NoArgsConstructor
public class PoliticaDescuento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_servicio", nullable = false, length = 20)
    private TipoServicio tipoServicio;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_descuento", nullable = false, length = 20)
    private TipoDescuento tipoDescuento;

    /** El porcentaje (de 0 a 100) o el monto en quetzales, según el tipo. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    /** Quién de la fundación la definió por última vez. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "registrada_por_id", nullable = false)
    private Usuario registradaPor;

    @UpdateTimestamp
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
