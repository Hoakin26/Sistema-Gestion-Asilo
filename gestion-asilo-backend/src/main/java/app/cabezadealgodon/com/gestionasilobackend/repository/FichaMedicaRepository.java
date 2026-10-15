package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.cabezadealgodon.com.gestionasilobackend.models.FichaMedica;

/**
 * YS-57. La única puerta a los datos clínicos de un interno.
 *
 * <p>
 * `Interno` no tiene un campo que lleve a su ficha, a propósito (RNF-03), así
 * que todo código que necesite datos clínicos tiene que pasar por aquí. Eso
 * deja el acceso a lo sensible visible en el código y revisable: si este
 * repositorio no aparece en un service, ese service no puede leer psicopatologías
 * ni medicamentos, por mucho que se equivoque armando el DTO.
 */
public interface FichaMedicaRepository extends JpaRepository<FichaMedica, Long> {

    Optional<FichaMedica> findByInternoId(Long internoId);
}
