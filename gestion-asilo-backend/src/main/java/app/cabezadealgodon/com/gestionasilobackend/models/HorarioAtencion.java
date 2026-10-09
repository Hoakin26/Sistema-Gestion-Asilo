package app.cabezadealgodon.com.gestionasilobackend.models;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-90. Un día de atención de un especialista: de qué hora a qué hora.
 *
 * <p>
 * Un solo bloque por día, que es lo que la restricción única garantiza. Cubre
 * el caso común —por ejemplo, lunes a viernes de 8:00 a 12:00— sin la
 * complejidad de bloques partidos, que ningún requerimiento pide.
 */
@Entity
@Table(name = "horario_atencion", uniqueConstraints = {
        @UniqueConstraint(name = "uk_horario_especialista_dia", columnNames = { "especialista_id", "dia" })
})
@Getter
@Setter
@NoArgsConstructor
public class HorarioAtencion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "especialista_id", nullable = false)
    private MedicoEspecialista especialista;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DayOfWeek dia;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;
}
