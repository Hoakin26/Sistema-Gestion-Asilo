package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

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
     * Los anulados también, para que se vea qué se corrigió.
     */
    @EntityGraph(attributePaths = { "registradoPor", "anuladoPor" })
    @Query("""
            select g from GastoOperativo g
            where g.fecha between :desde and :hasta and g.tipoGasto in :tipos
            order by g.fecha desc, g.id desc
            """)
    List<GastoOperativo> historico(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
            @Param("tipos") Collection<TipoGasto> tipos);
}
