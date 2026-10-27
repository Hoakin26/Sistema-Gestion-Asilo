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

import app.cabezadealgodon.com.gestionasilobackend.models.GastoOperativo;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoGasto;

public interface GastoOperativoRepository extends JpaRepository<GastoOperativo, Long> {

    /**
     * YS-148. El histórico de gastos (HU-23): los de alguno de {@code tipos}
     * entre dos fechas, las dos incluidas, del más reciente al más antiguo.
     * Los anulados también, para que se vea qué se corrigió. OBS-01: por
     * páginas, porque crece con cada mes.
     */
    @EntityGraph(attributePaths = { "registradoPor", "anuladoPor" })
    @Query(value = """
            select g from GastoOperativo g
            where g.fecha between :desde and :hasta and g.tipoGasto in :tipos
            order by g.fecha desc, g.id desc
            """, countQuery = """
            select count(g) from GastoOperativo g
            where g.fecha between :desde and :hasta and g.tipoGasto in :tipos
            """)
    Page<GastoOperativo> historico(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
            @Param("tipos") Collection<TipoGasto> tipos, Pageable paginado);

    /**
     * OBS-01. Lo que suman los gastos vigentes del mismo filtro, por tipo de
     * gasto: los totales del histórico, de todos y no solo de la página.
     */
    @Query("""
            select g.tipoGasto as tipo, count(g) as cantidad, sum(g.monto) as total
            from GastoOperativo g
            where g.anulado = false and g.fecha between :desde and :hasta and g.tipoGasto in :tipos
            group by g.tipoGasto
            """)
    List<TotalPorTipo> totalesDelHistorico(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
            @Param("tipos") Collection<TipoGasto> tipos);

    /** OBS-01. Una fila de {@link #totalesDelHistorico}: cuántos gastos de un tipo y cuánto suman. */
    interface TotalPorTipo {
        TipoGasto getTipo();

        Long getCantidad();

        BigDecimal getTotal();
    }
}
