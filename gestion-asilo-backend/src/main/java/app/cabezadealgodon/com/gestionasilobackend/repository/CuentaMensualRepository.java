package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.cabezadealgodon.com.gestionasilobackend.models.CuentaMensual;

public interface CuentaMensualRepository extends JpaRepository<CuentaMensual, Long> {

    Optional<CuentaMensual> findByInternoIdAndPeriodo(Long internoId, String periodo);

    /** YS-132. Las cuentas de un mes, por nombre del interno: "Cuentas por cobrar" (HU-18). */
    @EntityGraph(attributePaths = { "interno", "familiar" })
    @Query("select c from CuentaMensual c where c.periodo = :periodo order by c.interno.nombreCompleto, c.id")
    List<CuentaMensual> delPeriodo(@Param("periodo") String periodo);

    /**
     * YS-132. Las de un interno entre dos meses, del más antiguo al más
     * reciente. {@code between} sobre el texto sirve porque "yyyy-MM" ordena
     * igual como texto que como fecha.
     */
    @EntityGraph(attributePaths = { "interno", "familiar" })
    List<CuentaMensual> findByInternoIdAndPeriodoBetweenOrderByPeriodoAsc(Long internoId, String desde, String hasta);

    /** YS-132. Todas las de un interno: de ellas sale lo que debe en total. */
    List<CuentaMensual> findByInternoId(Long internoId);
}
