package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.cabezadealgodon.com.gestionasilobackend.models.EstadoNotificacion;
import app.cabezadealgodon.com.gestionasilobackend.models.NotificacionFamiliar;
import jakarta.persistence.LockModeType;

public interface NotificacionFamiliarRepository extends JpaRepository<NotificacionFamiliar, Long> {

    List<NotificacionFamiliar> findBySolicitudIdOrderByIdAsc(Long solicitudId);

    /**
     * Los avisos de una solicitud en ciertos estados, bloqueados hasta que
     * termine la transacción. El envío automático y un reintento manual pueden
     * coincidir sobre el mismo aviso; con el bloqueo, el segundo espera, ve que
     * ya quedó enviado y no le escribe dos veces al familiar.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select n from NotificacionFamiliar n
            where n.solicitud.id = :solicitudId and n.estado in :estados
            order by n.id
            """)
    List<NotificacionFamiliar> bloquearDeSolicitud(@Param("solicitudId") Long solicitudId,
            @Param("estados") Collection<EstadoNotificacion> estados);

    /**
     * Lo que el reintento automático tiene que volver a intentar: los fallidos
     * que no agotaron sus intentos, y los pendientes que llevan un rato sin
     * enviarse (el primer envío se perdió, por ejemplo porque la aplicación se
     * reinició en ese momento). Los recién creados no: de esos se encarga el
     * primer envío, y tomarlos aquí también los mandaría dos veces.
     */
    @Query("""
            select distinct n.solicitud.id from NotificacionFamiliar n
            where (n.estado = :fallida and n.intentos < :maxIntentos)
               or (n.estado = :pendiente and n.fechaCreacion < :pendientesAntesDe)
            """)
    List<Long> solicitudesParaReintentar(@Param("fallida") EstadoNotificacion fallida,
            @Param("pendiente") EstadoNotificacion pendiente,
            @Param("maxIntentos") int maxIntentos,
            @Param("pendientesAntesDe") LocalDateTime pendientesAntesDe);
}
