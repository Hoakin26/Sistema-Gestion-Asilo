package app.cabezadealgodon.com.gestionasilobackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import app.cabezadealgodon.com.gestionasilobackend.models.Interno;

public interface InternoRepository extends JpaRepository<Interno, Long> {

    boolean existsByDpi(String dpi);

    /**
     * La búsqueda de la lista de internos (HU-01). Por nombre o por DPI, que es
     * como el personal los identifica, y sin distinguir mayúsculas porque nadie
     * escribe un nombre dos veces igual.
     */
    Page<Interno> findByNombreCompletoContainingIgnoreCaseOrDpiContaining(
            String nombre, String dpi, Pageable paginado);
}
