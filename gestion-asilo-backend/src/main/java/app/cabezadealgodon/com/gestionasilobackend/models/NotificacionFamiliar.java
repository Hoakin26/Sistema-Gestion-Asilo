package app.cabezadealgodon.com.gestionasilobackend.models;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-95. Cada aviso por correo al familiar responsable y en qué quedó.
 *
 * <p>
 * Se registra en la misma transacción que la solicitud, antes de intentar
 * enviar nada. Así, si el servidor se cae justo después de guardar la
 * solicitud, el aviso no se pierde: queda pendiente y el reintento automático
 * lo encuentra.
 */
@Entity
@Table(name = "notificacion_familiar", indexes = {
        // El reintento automático busca por estado.
        @Index(name = "idx_notificacion_estado", columnList = "estado")
})
@Getter
@Setter
@NoArgsConstructor
public class NotificacionFamiliar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudReferencia solicitud;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoNotificacion estado = EstadoNotificacion.PENDIENTE;

    /**
     * A qué correo se envió en el último intento. No decide a quién se envía:
     * cada intento lee el correo actual del familiar, así que si estaba mal
     * escrito y alguien lo corrige, el reintento ya va al correcto.
     */
    @Column(length = 150)
    private String destinatario;

    @Column(nullable = false)
    private int intentos;

    /** El motivo de la última falla, para diagnosticarla. Nunca sale por la API. */
    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_ultimo_intento")
    private LocalDateTime fechaUltimoIntento;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;
}
