package app.cabezadealgodon.com.gestionasilobackend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * YS-83. El último correlativo de solicitud usado en cada año, para numerar
 * "REF-2026-0001", "REF-2026-0002"… y reiniciar en enero.
 *
 * <p>
 * Una fila por año en vez de calcular el máximo de las solicitudes existentes:
 * con el máximo, dos médicos que crean una solicitud en el mismo instante leen
 * el mismo valor y obtienen el mismo número. Con esta fila, el servicio la
 * bloquea antes de leerla y la segunda creación espera a que la primera
 * termine (ver {@code ContadorSolicitudRepository}).
 */
@Entity
@Table(name = "contador_solicitud")
@Getter
@Setter
@NoArgsConstructor
public class ContadorSolicitud {

    @Id
    private Integer anio;

    @Column(nullable = false)
    private int ultimo;
}
