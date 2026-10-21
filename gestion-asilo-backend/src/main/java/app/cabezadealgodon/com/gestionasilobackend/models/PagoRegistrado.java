package app.cabezadealgodon.com.gestionasilobackend.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * YS-139. Lo que tienen en común un pago del familiar al asilo ({@link Pago},
 * HU-34) y un pago del asilo a la fundación ({@link PagoFundacion}, HU-20): la
 * fecha, el monto, la forma de pago, la referencia, quién lo registró y su
 * anulación. Cada uno lo aplica a su cuenta; esta clase no sabe a cuál.
 *
 * <p>
 * {@code @MappedSuperclass}: no tiene tabla propia. Cada pago guarda estas
 * columnas en la suya, con los mismos nombres que tenía {@code pago} antes de
 * HU-20, así que esa tabla no cambia.
 *
 * <p>
 * YS-176. Un pago no se borra ni se edita: es un registro financiero. Si se
 * registró mal, se anula, con el motivo y quién lo anuló, y queda en el
 * historial (decidido el 20/10). Uno anulado ya no cuenta para el saldo.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class PagoRegistrado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    /** El día en que se pagó; puede ser anterior a cuando se registró. */
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
