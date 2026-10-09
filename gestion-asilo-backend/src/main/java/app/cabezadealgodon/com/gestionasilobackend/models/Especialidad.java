package app.cabezadealgodon.com.gestionasilobackend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-83. Una especialidad médica que ofrece la fundación, a la que el Médico
 * General puede referir a un interno (RF-03).
 *
 * <p>
 * Es una entidad y no una enumeración, a diferencia de los roles: el catálogo
 * es de la fundación, no del sistema, y va a cambiar cuando cambien los
 * especialistas con que trabaja. El diseño prevé además asociarle la tarifa de
 * consulta (hallazgo 3 de la sección 5 del diseño), que llega con la visita
 * médica.
 */
@Entity
@Table(name = "especialidad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Especialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    /**
     * Las especialidades no se eliminan, se desactivan: las solicitudes viejas
     * tienen que seguir diciendo a qué especialidad se refirió al interno.
     */
    @Column(nullable = false)
    private boolean activa = true;
}
