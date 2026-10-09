package app.cabezadealgodon.com.gestionasilobackend.models;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import app.cabezadealgodon.com.gestionasilobackend.exception.TransicionInvalidaException;
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
 * YS-83. La solicitud con que el Médico General refiere a un interno a una
 * especialidad de la fundación, con un enfermero(a) acompañante (RF-03,
 * CU-001).
 *
 * <p>
 * Es el primer eslabón de la cadena solicitud → cita → visita (diseño, sección
 * 3.4): son tres actores en tres momentos distintos, y separarlos le da a cada
 * uno su propia bandeja y sus propios permisos.
 */
@Entity
@Table(name = "solicitud_referencia", indexes = {
        // La bandeja de la fundación (HU-05) filtra por estado.
        @Index(name = "idx_solicitud_estado", columnList = "estado")
})
@Getter
@Setter
@NoArgsConstructor
public class SolicitudReferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** "REF-2026-0001": lo que el personal cita para darle seguimiento. */
    @Column(nullable = false, unique = true, length = 20, updatable = false)
    private String numero;

    @ManyToOne(optional = false)
    @JoinColumn(name = "interno_id", nullable = false)
    private Interno interno;

    @ManyToOne(optional = false)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    /** Quien refiere. Lo toma el servicio del token, nunca del cuerpo de la petición. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "medico_general_id", nullable = false)
    private Usuario medicoGeneral;

    /** Quien acompaña al interno a la cita. No es quien crea la solicitud (RF-03). */
    @ManyToOne(optional = false)
    @JoinColumn(name = "enfermero_acompanante_id", nullable = false)
    private Usuario enfermeroAcompanante;

    @Column(nullable = false, length = 500)
    private String motivo;

    /**
     * Sin setter público: el estado solo cambia por {@link #cambiarEstado}, que
     * es el único lugar donde se respeta el diagrama DE-01. Nace en
     * PENDIENTE_ASIGNACION, y no hay forma de crearla en otro estado.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Setter(AccessLevel.NONE)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE_ASIGNACION;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    /** Lleva la solicitud a otro estado, si el diagrama DE-01 lo permite. */
    public void cambiarEstado(EstadoSolicitud nuevo) {
        if (!estado.puedeCambiarA(nuevo)) {
            throw new TransicionInvalidaException("La solicitud " + numero + " está en " + estado
                    + " y no puede pasar a " + nuevo);
        }
        estado = nuevo;
    }
}
