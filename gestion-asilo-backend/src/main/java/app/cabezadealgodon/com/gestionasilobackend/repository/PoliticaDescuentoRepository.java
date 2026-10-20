package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import app.cabezadealgodon.com.gestionasilobackend.models.PoliticaDescuento;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;

public interface PoliticaDescuentoRepository extends JpaRepository<PoliticaDescuento, Long> {

    /** YS-136. Las políticas de un servicio, de la más reciente a la más antigua. */
    List<PoliticaDescuento> findByTipoServicioOrderByVigenteDesdeDesc(TipoServicio tipoServicio);

    /** YS-136. Todas, por servicio y de la más reciente a la más antigua: la pantalla y la consolidación. */
    @EntityGraph(attributePaths = { "registradaPor" })
    List<PoliticaDescuento> findAllByOrderByTipoServicioAscVigenteDesdeDesc();

    Optional<PoliticaDescuento> findByTipoServicioAndVigenteDesde(TipoServicio tipoServicio, LocalDate vigenteDesde);
}
