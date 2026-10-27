package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.cabezadealgodon.com.gestionasilobackend.models.Donacion;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoDonante;

public interface DonacionRepository extends JpaRepository<Donacion, Long> {

    /**
     * YS-145. El histórico de donaciones (HU-22): las de alguno de
     * {@code tipos} entre dos fechas, las dos incluidas, de la más reciente a
     * la más antigua. Las anuladas también, para que se vea qué se corrigió.
     * OBS-01: por páginas, porque crece con cada mes.
     */
    @EntityGraph(attributePaths = { "registradaPor", "anuladaPor" })
    @Query(value = """
            select d from Donacion d
            where d.fecha between :desde and :hasta and d.tipoDonante in :tipos
            order by d.fecha desc, d.id desc
            """, countQuery = """
            select count(d) from Donacion d
            where d.fecha between :desde and :hasta and d.tipoDonante in :tipos
            """)
    Page<Donacion> historico(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
            @Param("tipos") Collection<TipoDonante> tipos, Pageable paginado);

    /**
     * OBS-01. Lo que suman las donaciones vigentes del mismo filtro, por tipo
     * de donante: los totales del histórico, de todas y no solo de la página.
     */
    @Query("""
            select d.tipoDonante as tipo, count(d) as cantidad, sum(d.monto) as total
            from Donacion d
            where d.anulada = false and d.fecha between :desde and :hasta and d.tipoDonante in :tipos
            group by d.tipoDonante
            """)
    List<TotalPorTipo> totalesDelHistorico(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
            @Param("tipos") Collection<TipoDonante> tipos);

    /**
     * YS-160. Las donaciones recibidas entre dos fechas, las dos incluidas,
     * para el reporte de entradas (HU-27, RF-20b): sin las anuladas, que no
     * entraron, de la más antigua a la más reciente.
     */
    @Query("""
            select d from Donacion d
            where d.anulada = false and d.fecha between :desde and :hasta
            order by d.fecha, d.id
            """)
    List<Donacion> recibidasEntre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** OBS-01. Una fila de {@link #totalesDelHistorico}: cuántas donaciones de un tipo y cuánto suman. */
    interface TotalPorTipo {
        TipoDonante getTipo();

        Long getCantidad();

        BigDecimal getTotal();
    }
}
