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
 * YS-57. El familiar que responde por un interno: quien recibe las cuentas y a
 * quien se avisa de lo que ocurra con él.
 *
 * <p>
 * Es una entidad propia y no unos campos dentro de {@code Interno} porque una
 * misma persona puede responder por varios internos —hermanos, un matrimonio—,
 * y repetir sus datos dejaría que una copia quedara desactualizada frente a la
 * otra.
 */
@Entity
@Table(name = "familiar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Familiar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(nullable = false, length = 50)
    private String parentesco;

    @Column(nullable = false, length = 20)
    private String telefono;

    /** Opcional: no todo familiar responsable tiene correo electrónico. */
    @Column(length = 150)
    private String correo;

    @Column(length = 200)
    private String direccion;
}
