package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.cabezadealgodon.com.gestionasilobackend.models.DetalleReceta;

public interface DetalleRecetaRepository extends JpaRepository<DetalleReceta, Long> {

    /**
     * YS-163. Los medicamentos que la farmacia le entregó a un interno entre
     * dos momentos, por la fecha de entrega, para su reporte (HU-28, RF-21b).
     * Con la receta y quién la emitió de una vez.
     */
    @EntityGraph(attributePaths = { "receta", "receta.visita", "receta.visita.cita", "receta.visita.cita.solicitud",
            "receta.visita.cita.solicitud.especialidad", "receta.visita.especialista",
            "receta.visita.especialista.usuario" })
    @Query("""
            select d from DetalleReceta d
            where d.receta.visita.interno.id = :internoId and d.entregado = true
              and d.fechaEntrega >= :desde and d.fechaEntrega < :hasta
            order by d.fechaEntrega, d.id
            """)
    List<DetalleReceta> entregadosAlInternoEntre(@Param("internoId") Long internoId,
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
