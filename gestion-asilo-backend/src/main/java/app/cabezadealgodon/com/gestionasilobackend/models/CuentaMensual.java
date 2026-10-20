package app.cabezadealgodon.com.gestionasilobackend.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-132. La cuenta mensual del familiar (HU-18, RF-12, CU-006 pasos 1 a 4):
 * la cuota base del interno más los cargos del mes por consultas, laboratorio
 * y farmacia, a cargo de su familiar responsable.
 *
 * <p>
 * Una por interno y período ({@code uniqueConstraints}): consolidar dos veces
 * el mismo mes la recalcula, no la duplica. Los cargos no se copian aquí; cada
 * {@link CargoServicio} apunta a la cuenta en que se consolidó, como en el
 * diseño ("consolidado en"), y los totales son la suma de esos cargos.
 *
 * <p>
 * El familiar se guarda en la cuenta y no se lee del interno al consultarla:
 * si el responsable cambia, la cuenta de un mes ya cobrado sigue diciendo a
 * quién se le cobró. Lo mismo la cuota base.
 *
 * <p>
 * {@code saldo} es lo que falta pagar. Hoy es igual al total, porque los pagos
 * llegan con HU-34 (RF-22), que es la que lo descuenta y cambia el estado
 * (DE-04). {@code version} queda lista para ese momento: dos pagos o un pago y
 * una reconsolidación simultáneos no se pisarán.
 */
@Entity
@Table(name = "cuenta_mensual", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cuenta_interno_periodo", columnNames = { "interno_id", "periodo" })
}, indexes = {
        // "Cuentas por cobrar" (HU-18) lista las de un período.
        @Index(name = "idx_cuenta_periodo", columnList = "periodo")
})
@Getter
@Setter
@NoArgsConstructor
public class CuentaMensual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "interno_id", nullable = false)
    private Interno interno;

    /** El familiar responsable al consolidar: a quien se le cobra (diseño, "a cargo de"). */
    @ManyToOne(optional = false)
    @JoinColumn(name = "familiar_id", nullable = false)
    private Familiar familiar;

    /** "2026-10": el mes que cubre, en el formato de {@code YearMonth}. */
    @Column(nullable = false, length = 7)
    private String periodo;

    /** La cuota mensual del interno; cero si no tiene una definida. */
    @Column(name = "cuota_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal cuotaBase;

    /** La suma de los montos originales de los cargos del mes. */
    @Column(name = "total_servicios", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalServicios;

    /** La suma de sus descuentos (HU-19); cero mientras no haya políticas. */
    @Column(name = "total_descuentos", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalDescuentos;

    /** Cuota base más los cargos netos del mes (regla de negocio de HU-18). */
    @Column(name = "total_a_pagar", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAPagar;

    /** Lo que falta pagar. HU-34 le resta los pagos. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal saldo;

    /** Sin setter: las transiciones de DE-04 llegan con HU-34. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Setter(AccessLevel.NONE)
    private EstadoCuenta estado = EstadoCuenta.PENDIENTE_PAGO;

    /** La última vez que se consolidó. */
    @Column(name = "fecha_consolidacion", nullable = false)
    private LocalDateTime fechaConsolidacion;

    @Version
    private Long version;
}
