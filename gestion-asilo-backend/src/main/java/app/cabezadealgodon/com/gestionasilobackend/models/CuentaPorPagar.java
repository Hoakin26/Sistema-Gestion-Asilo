package app.cabezadealgodon.com.gestionasilobackend.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-139. Lo que el asilo le debe a la fundación por un tipo de servicio en un
 * mes (HU-20, RF-13): las consultas de octubre, los exámenes de octubre, la
 * farmacia de octubre. Decidido el 21/10: una deuda por mes y servicio, para
 * que cada pago lleve su concepto, como pide el reporte de pagos a la
 * fundación (RF-20a).
 *
 * <p>
 * <b>Lo adeudado no se guarda.</b> Decidido el 21/10: es siempre la suma del
 * neto de los cargos de ese servicio en ese mes, con el descuento de la
 * fundación, porque el descuento lo da ella. Así un examen que se carga tarde
 * entra solo, sin consolidar nada. Aquí se guarda lo pagado.
 *
 * <p>
 * La fila nace con el primer pago: un mes sin pagos no la necesita. Una por
 * mes y servicio ({@code uniqueConstraints}), y con {@code version}, como la
 * cuenta del familiar: dos pagos simultáneos no pasan juntos del saldo.
 */
@Entity
@Table(name = "cuenta_por_pagar", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cuenta_por_pagar_periodo_tipo", columnNames = { "periodo", "tipo_servicio" })
})
@Getter
@Setter
@NoArgsConstructor
public class CuentaPorPagar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** "2026-10": el mes de los servicios, en el formato de {@code YearMonth}. */
    @Column(nullable = false, length = 7)
    private String periodo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_servicio", nullable = false, length = 20)
    private TipoServicio tipoServicio;

    /** Lo pagado a la fundación, sin los pagos anulados. Sin setter: sale de los pagos. */
    @Column(name = "total_pagado", nullable = false, precision = 10, scale = 2)
    @Setter(AccessLevel.NONE)
    private BigDecimal totalPagado = BigDecimal.ZERO.setScale(2);

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Version
    private Long version;

    /** Suma un pago; el servicio ya comprobó que no pase del saldo. */
    public void registrarPago(BigDecimal monto) {
        totalPagado = totalPagado.add(monto).setScale(2);
    }

    /** Resta un pago anulado: su monto vuelve al saldo. */
    public void anularPago(BigDecimal monto) {
        totalPagado = totalPagado.subtract(monto).max(BigDecimal.ZERO).setScale(2);
    }
}
