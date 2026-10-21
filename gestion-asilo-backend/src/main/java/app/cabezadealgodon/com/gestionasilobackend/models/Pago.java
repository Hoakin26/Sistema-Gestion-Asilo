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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-133. Un pago del familiar sobre su cuenta mensual (HU-34, RF-22, CU-006
 * paso 6): la fecha, el monto y la forma de pago. Se aplica a la cuenta
 * entera, cuota más servicios con descuento, no a un servicio en particular;
 * lo que dice de cada servicio sale del estado de la cuenta (DE-04).
 *
 * <p>
 * YS-176. Un pago no se borra ni se edita: es un registro financiero. Si se
 * registró mal, se anula, con el motivo y quién lo anuló, y queda en el
 * historial (decidido el 20/10). Uno anulado ya no cuenta para el saldo.
 */
@Entity
@Table(name = "pago", indexes = {
        @Index(name = "idx_pago_cuenta", columnList = "cuenta_id"),
        // Los reportes de cobros y de entradas (HU-26, HU-27) van por fecha.
        @Index(name = "idx_pago_fecha", columnList = "fecha")
})
@Getter
@Setter
@NoArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private CuentaMensual cuenta;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    /** El día en que el familiar pagó; puede ser anterior a cuando se registró. */
    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pago", nullable = false, length = 20)
    private FormaPago formaPago;

    /** El número de boleta, de transferencia o de cheque, si lo hay. */
    @Column(length = 100)
    private String referencia;

    @ManyToOne(optional = false)
    @JoinColumn(name = "registrado_por_id", nullable = false)
    private Usuario registradoPor;

    @CreationTimestamp
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    /** YS-176. Si se anuló. Sin setter: solo cambia por {@link #anular}. */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
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

    /** YS-176. Lo anula; el servicio ya comprobó que no lo estuviera. */
    public void anular(String motivo, Usuario usuario, LocalDateTime momento) {
        anulado = true;
        motivoAnulacion = motivo;
        anuladoPor = usuario;
        fechaAnulacion = momento;
    }
}
