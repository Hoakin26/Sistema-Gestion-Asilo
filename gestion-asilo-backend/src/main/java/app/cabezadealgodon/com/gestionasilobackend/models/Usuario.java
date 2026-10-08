package app.cabezadealgodon.com.gestionasilobackend.models;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un usuario del sistema (no confundir con "interno"/residente del asilo).
 * La contraseña nunca se guarda en texto plano: passwordHash contiene el
 * resultado de BCrypt (ver docs/seguridad.md), aplicado en el service que
 * registra al usuario (YS-41/YS-42), no en esta entidad.
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    /**
     * Momento a partir del cual los tokens emitidos antes dejan de valer.
     * Lo mueve el cierre de sesión (YS-68); nulo significa que nunca ha
     * cerrado sesión.
     */
    @Column(name = "fecha_invalidacion_sesiones")
    private LocalDateTime fechaInvalidacionSesiones;

    /**
     * YS-74. Verdadero mientras el usuario siga usando una contraseña que
     * eligió otra persona: la del alta, la de un restablecimiento o la que el
     * sistema generó al sembrar el primer administrador. Solo la apaga el
     * cambio de contraseña propio, que exige la anterior y por lo tanto solo
     * puede hacerlo su dueño.
     *
     * El valor por defecto va en la definición de la columna y no solo en Java:
     * la tabla ya tiene filas, y un ALTER TABLE que agregue una columna NOT
     * NULL sin valor por defecto lo rechaza PostgreSQL. Con esto las filas
     * existentes se rellenan solas y nacen sin la marca, que es lo correcto:
     * ninguna de esas contraseñas la conoce el administrador.
     */
    @Column(name = "debe_cambiar_password", nullable = false,
            columnDefinition = "boolean not null default false")
    private boolean debeCambiarPassword = false;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;
}
