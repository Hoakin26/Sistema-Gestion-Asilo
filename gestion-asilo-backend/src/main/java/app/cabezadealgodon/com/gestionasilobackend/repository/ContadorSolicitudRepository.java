package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.cabezadealgodon.com.gestionasilobackend.models.ContadorSolicitud;
import jakarta.persistence.LockModeType;

/**
 * YS-83. El contador del correlativo. Se usa siempre en este orden y dentro de
 * la misma transacción: {@link #asegurarAnio} y después {@link #bloquear}.
 */
public interface ContadorSolicitudRepository extends JpaRepository<ContadorSolicitud, Integer> {

    /**
     * Crea la fila del año si todavía no existe. Con {@code on conflict do
     * nothing}, la primera solicitud de enero creada por dos médicos a la vez
     * no choca: una de las dos inserta y la otra no hace nada.
     */
    @Modifying
    @Query(value = "insert into contador_solicitud (anio, ultimo) values (:anio, 0) on conflict (anio) do nothing",
            nativeQuery = true)
    void asegurarAnio(@Param("anio") int anio);

    /**
     * Lee la fila con {@code select … for update}: mientras esta transacción no
     * termine, cualquier otra que pida la misma fila espera. Es lo que impide
     * que dos solicitudes simultáneas obtengan el mismo número.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ContadorSolicitud c where c.anio = :anio")
    Optional<ContadorSolicitud> bloquear(@Param("anio") int anio);
}
