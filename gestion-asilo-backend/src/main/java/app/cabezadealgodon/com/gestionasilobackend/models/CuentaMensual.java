package app.cabezadealgodon.com.gestionasilobackend.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

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
 * {@code saldo} es lo que falta pagar: el total menos lo pagado (HU-34,
 * RF-22). {@code version} activa el bloqueo optimista: dos pagos, o un pago y
 * una reconsolidación, simultáneos no se pisan; el segundo recibe 409.
 *
 * <p>
 * YS-133. El estado (DE-04) sale del saldo, de lo pagado y de la fecha, y no
 * se cambia a mano: ver {@link #estadoAl}. Se guarda cada vez que la cuenta
 * cambia, y al leerla se vuelve a calcular con la fecha del día, porque la mora
 * llega sola con el calendario, sin que nadie toque la cuenta.
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

    /**
     * YS-133. Lo pagado por el familiar, sin los pagos anulados (HU-34). El
     * valor por defecto va en la columna, como en {@code Usuario}: la tabla ya
     * tiene filas, y las cuentas que existían no tenían pagos.
     */
    @Column(name = "total_pagado", nullable = false, columnDefinition = "numeric(10,2) not null default 0")
    @Setter(AccessLevel.NONE)
    private BigDecimal totalPagado = BigDecimal.ZERO.setScale(2);

    /** Lo que falta pagar: el total menos lo pagado. Sin setter: sale de {@link #actualizarSaldo}. */
    @Column(nullable = false, precision = 10, scale = 2)
    @Setter(AccessLevel.NONE)
    private BigDecimal saldo;

    /** Sin setter: sale del saldo, de lo pagado y de la fecha ({@link #estadoAl}). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Setter(AccessLevel.NONE)
    private EstadoCuenta estado = EstadoCuenta.PENDIENTE_PAGO;

    /** La última vez que se consolidó. */
    @Column(name = "fecha_consolidacion", nullable = false)
    private LocalDateTime fechaConsolidacion;

    @Version
    private Long version;

    /**
     * YS-176. El último día para pagarla: el fin del mes siguiente a su período
     * (decidido el 20/10). La de octubre vence el 30 de noviembre.
     */
    public LocalDate getFechaVencimiento() {
        return YearMonth.parse(periodo).plusMonths(1).atEndOfMonth();
    }

    /** YS-133. Si tiene pagos vigentes; los anulados no cuentan. */
    public boolean tienePagos() {
        return totalPagado.signum() > 0;
    }

    /**
     * YS-133. El estado de la cuenta en {@code dia} (DE-04):
     *
     * <ul>
     * <li>sin saldo, Completamente Cancelada: el pago total cancela los
     * servicios incluidos (HU-34);</li>
     * <li>con saldo y ya vencida, En Mora, haya pagado una parte o no: un pago
     * parcial no la saca de la mora (YS-176);</li>
     * <li>con saldo y sin vencer, Pago Parcial si ya pagó algo, o Pendiente de
     * Pago si todavía no.</li>
     * </ul>
     */
    public EstadoCuenta estadoAl(LocalDate dia) {
        if (saldo.signum() == 0) {
            return EstadoCuenta.COMPLETAMENTE_CANCELADA;
        }
        if (dia.isAfter(getFechaVencimiento())) {
            return EstadoCuenta.EN_MORA;
        }
        return tienePagos() ? EstadoCuenta.PAGO_PARCIAL : EstadoCuenta.PENDIENTE_PAGO;
    }

    /** YS-133. El saldo con el total y lo pagado de ahora, y el estado que le toca hoy. */
    public void actualizarSaldo(LocalDate hoy) {
        saldo = totalAPagar.subtract(totalPagado).max(BigDecimal.ZERO).setScale(2);
        estado = estadoAl(hoy);
    }

    /** YS-133. Suma un pago; el servicio ya comprobó que no pase del saldo. */
    public void registrarPago(BigDecimal monto, LocalDate hoy) {
        totalPagado = totalPagado.add(monto).setScale(2);
        actualizarSaldo(hoy);
    }

    /** YS-176. Resta un pago anulado: su monto vuelve al saldo. */
    public void anularPago(BigDecimal monto, LocalDate hoy) {
        totalPagado = totalPagado.subtract(monto).max(BigDecimal.ZERO).setScale(2);
        actualizarSaldo(hoy);
    }
}
