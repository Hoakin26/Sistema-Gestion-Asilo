package app.cabezadealgodon.com.gestionasilobackend.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-139. Un pago del asilo a la fundación (HU-20, RF-13), la entidad
 * {@code PagoFundacion} del diseño. Va a una deuda de un mes y un servicio, así
 * que su concepto (consulta, laboratorio o farmacia) es el de su
 * {@link CuentaPorPagar}. Los campos y la anulación son los de
 * {@link PagoRegistrado}, como el pago del familiar.
 */
@Entity
@Table(name = "pago_fundacion", indexes = {
        @Index(name = "idx_pago_fundacion_cuenta", columnList = "cuenta_id"),
        // El reporte de pagos a la fundación (HU-27, RF-20a) va por fecha.
        @Index(name = "idx_pago_fundacion_fecha", columnList = "fecha")
})
@Getter
@Setter
@NoArgsConstructor
public class PagoFundacion extends PagoRegistrado {

    @ManyToOne(optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private CuentaPorPagar cuenta;
}
