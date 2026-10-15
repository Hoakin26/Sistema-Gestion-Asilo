package app.cabezadealgodon.com.gestionasilobackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.cabezadealgodon.com.gestionasilobackend.models.Interno;

public interface InternoRepository extends JpaRepository<Interno, Long> {

    boolean existsByDpi(String dpi);

    /**
     * La búsqueda de la lista de internos (HU-01). Por nombre o por DPI, que es
     * como el personal los identifica, sin distinguir mayúsculas ni tildes:
     * nadie escribe un nombre dos veces igual, y en la tableta lo normal es
     * escribir "nunez" para buscar a "Núñez" (YS-62).
     *
     * <p>
     * {@code translate} es de PostgreSQL: cambia cada letra con tilde por su
     * letra base antes de pasar a minúsculas. Va en ese orden a propósito: con
     * una base en locale C, {@code lower} no sabe bajar una "Á", pero una "A"
     * sí. Los patrones llegan ya normalizados de la misma forma desde
     * {@code InternoService}. Se prefirió a la extensión {@code unaccent}
     * porque esa hay que instalarla en cada base a mano, y con
     * {@code ddl-auto} nadie lo haría.
     */
    @Query("""
            select i from Interno i
            where lower(function('translate', i.nombreCompleto,
                        'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN')) like :nombre escape '\\'
               or i.dpi like :dpi escape '\\'
            """)
    Page<Interno> buscar(@Param("nombre") String patronNombre, @Param("dpi") String patronDpi,
            Pageable paginado);
}
