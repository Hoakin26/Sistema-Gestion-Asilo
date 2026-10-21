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
 * YS-133. Un pago del familiar sobre su cuenta mensual (HU-34, RF-22, CU-006
 * paso 6): la fecha, el monto y la forma de pago. Se aplica a la cuenta
 * entera, cuota más servicios con descuento, no a un servicio en particular;
 * lo que dice de cada servicio sale del estado de la cuenta (DE-04).
 *
 * <p>
 * YS-139. Los campos del pago y su anulación (YS-176) están en
 * {@link PagoRegistrado}, que comparte con el pago a la fundación (HU-20).
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
public class Pago extends PagoRegistrado {

    @ManyToOne(optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private CuentaMensual cuenta;
}
