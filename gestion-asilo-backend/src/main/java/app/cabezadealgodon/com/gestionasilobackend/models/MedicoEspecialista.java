package app.cabezadealgodon.com.gestionasilobackend.models;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-90. El perfil de un médico especialista de la fundación: con qué cuenta
 * entra al sistema, qué especialidad atiende y en qué horario.
 *
 * <p>
 * Es una entidad aparte del {@link Usuario} y no unos campos más en él porque
 * solo los especialistas tienen especialidad y horario: agregarlos al usuario
 * dejaría esos campos vacíos en los otros seis roles.
 *
 * <p>
 * La duración de la cita va aquí y no en cada día del horario, como sugería el
 * diagrama de clases: un especialista atiende citas de la misma duración todos
 * los días, y así las franjas de una semana quedan alineadas entre sí.
 */
@Entity
@Table(name = "medico_especialista")
@Getter
@Setter
@NoArgsConstructor
public class MedicoEspecialista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** La cuenta con que entra al sistema. Una cuenta, un perfil. */
    @OneToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @Column(name = "duracion_cita_minutos", nullable = false)
    private int duracionCitaMinutos;

    /**
     * Los especialistas no se eliminan, se desactivan: sus citas pasadas
     * tienen que seguir diciendo quién atendió. Uno inactivo no recibe citas
     * nuevas.
     */
    @Column(nullable = false)
    private boolean activo = true;

    @OneToMany(mappedBy = "especialista", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HorarioAtencion> horarios = new ArrayList<>();

    public void agregarHorario(HorarioAtencion horario) {
        horario.setEspecialista(this);
        horarios.add(horario);
    }

    /** El bloque de atención de un día, si ese día atiende. */
    public Optional<HorarioAtencion> horarioDe(DayOfWeek dia) {
        return horarios.stream().filter(horario -> horario.getDia() == dia).findFirst();
    }
}
