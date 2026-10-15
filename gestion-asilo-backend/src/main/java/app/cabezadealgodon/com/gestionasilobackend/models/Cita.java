package app.cabezadealgodon.com.gestionasilobackend.models;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-90. La cita que la Fundación le asigna a una solicitud de referencia:
 * con qué especialista y cuándo (RF-05, CU-002).
 *
 * <p>
 * Las dos restricciones únicas son la última red contra un choque que el
 * servicio ya impide: una solicitud no tiene dos citas, y un especialista no
 * tiene dos citas que empiecen a la misma hora.
 */
@Entity
@Table(name = "cita", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cita_especialista_hora", columnNames = { "especialista_id", "fecha_hora" })
})
@Getter
@Setter
@NoArgsConstructor
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false, unique = true)
    private SolicitudReferencia solicitud;

    @ManyToOne(optional = false)
    @JoinColumn(name = "especialista_id", nullable = false)
    private MedicoEspecialista especialista;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    /**
     * Copiada del especialista al asignar: si después cambia la duración de sus
     * citas, las ya dadas conservan la que tenían, y el cálculo de horas
     * libres sigue sabiendo cuánto ocupa cada una.
     */
    @Column(name = "duracion_minutos", nullable = false)
    private int duracionMinutos;

    /** Quién de la Fundación la asignó. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "asignada_por_id", nullable = false)
    private Usuario asignadaPor;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public LocalDateTime fechaHoraFin() {
        return fechaHora.plusMinutes(duracionMinutos);
    }
}
