package app.cabezadealgodon.com.gestionasilobackend.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-57. El residente del asilo: únicamente sus datos personales,
 * administrativos y su familiar responsable.
 *
 * <p>
 * <b>Aquí no hay ningún dato clínico, y es deliberado (RNF-03).</b> El
 * Administrador del Asilo no puede leer psicopatologías, diagnósticos ni
 * medicamentos, y la forma más sólida de garantizarlo no es ocultarlos al
 * serializar sino que esta entidad no tenga por dónde alcanzarlos.
 *
 * <p>
 * Por eso tampoco existe aquí un campo {@code fichaMedica}: la relación la
 * posee {@link FichaMedica}, que apunta al interno. Quien necesite los datos
 * clínicos tiene que pedirlos explícitamente por el repositorio de fichas, lo
 * cual deja la decisión a la vista en el código en lugar de depender de que
 * alguien recuerde excluir un campo del DTO. Un {@code @OneToOne} marcado como
 * perezoso no habría servido: del lado inverso Hibernate consulta igual, y
 * además dejaría el camino abierto a quien escriba el próximo endpoint.
 */
@Entity
@Table(name = "interno", indexes = {
        // El documento de diseño (sección 2) pide índices en los campos de
        // búsqueda: la lista de internos se filtra por nombre y por DPI.
        @Index(name = "idx_interno_nombre", columnList = "nombre_completo"),
        @Index(name = "idx_interno_dpi", columnList = "dpi")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Interno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    /** Documento Personal de Identificación: 13 dígitos en Guatemala. */
    @Column(nullable = false, unique = true, length = 13)
    private String dpi;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    /**
     * Cuota base que se le cobra al familiar cada mes. Es `BigDecimal` y no
     * `double`: con punto flotante los centavos se pierden al sumar, y esto
     * termina en el estado de cuenta de una familia.
     *
     * <p>
     * YS-59. Opcional al registrar. Quien da de alta al interno es el Médico
     * General (HU-01), y la cuota es un dato financiero que no es de su
     * competencia (RNF-02): exigirla lo obligaría a conocer algo que no le
     * corresponde o a inventar un valor provisorio para poder registrar al
     * paciente.
     *
     * <p>
     * Hoy solo puede completarse actualizando el interno, que también es del
     * Médico General. La vía para que la fije el Administrador del Asilo, que
     * es a quien le corresponde, llega con el módulo financiero (versión 1.1).
     */
    @Column(name = "cuota_mensual", precision = 10, scale = 2)
    private BigDecimal cuotaMensual;

    /**
     * Los internos no se eliminan, se marcan como inactivos, por lo mismo que
     * los usuarios: su historial clínico y sus cargos siguen existiendo y
     * tienen que seguir teniendo a quién pertenecer.
     */
    @Column(nullable = false)
    private boolean activo = true;

    /**
     * Obligatorio: no se registra a un interno sin alguien que responda por él.
     * La relación es hacia el familiar porque uno solo puede responder por
     * varios internos.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "familiar_responsable_id", nullable = false)
    private Familiar familiarResponsable;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;
}
