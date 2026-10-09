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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-98. La visita médica: la atención del especialista que nace de una cita
 * programada (HU-07, RF-06, CU-003). Es la base de HU-07 a HU-11: sobre ella
 * se registran el diagnóstico, la receta y los exámenes de laboratorio.
 *
 * <p>
 * Es un dato clínico (RNF-03): ningún endpoint la expone al Administrador del
 * Asilo.
 *
 * <p>
 * El interno y el especialista se podrían alcanzar desde la cita, pero se
 * guardan aquí también: HU-08 pregunta "¿tiene este especialista una visita
 * activa con este interno?", y con ellos esa pregunta es una consulta directa
 * sobre esta tabla y no un recorrido por tres.
 */
@Entity
@Table(name = "visita_medica", indexes = {
        @Index(name = "idx_visita_especialista_estado", columnList = "especialista_id, estado"),
        @Index(name = "idx_visita_interno", columnList = "interno_id")
})
@Getter
@Setter
@NoArgsConstructor
public class VisitaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Una cita da lugar a una sola visita: la restricción única es la última red. */
    @OneToOne(optional = false)
    @JoinColumn(name = "cita_id", nullable = false, unique = true)
    private Cita cita;

    @ManyToOne(optional = false)
    @JoinColumn(name = "interno_id", nullable = false)
    private Interno interno;

    @ManyToOne(optional = false)
    @JoinColumn(name = "especialista_id", nullable = false)
    private MedicoEspecialista especialista;

    /** HU-07: la fecha de la visita es el momento en que el especialista la inicia. */
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    /** HU-09. Vacía mientras la visita está activa. */
    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    /** HU-09. */
    @Column(length = 2000)
    private String diagnostico;

    /** HU-09. */
    @Column(length = 2000)
    private String observaciones;

    /**
     * YS-169. La tarifa de la especialidad, copiada al iniciar la visita: si
     * la fundación la cambia después, esta visita conserva lo que costaba
     * cuando se dio. Vacía si la especialidad todavía no tenía tarifa. La
     * suman la cuenta del familiar (RF-12), las cuentas por pagar (RF-13) y
     * el reporte de costos (RF-17).
     */
    @Column(name = "costo_consulta", precision = 10, scale = 2)
    private BigDecimal costoConsulta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVisita estado = EstadoVisita.ACTIVA;
}
