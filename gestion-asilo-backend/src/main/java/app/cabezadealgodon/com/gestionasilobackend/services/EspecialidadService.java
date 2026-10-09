package app.cabezadealgodon.com.gestionasilobackend.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.EspecialidadResponse;
import app.cabezadealgodon.com.gestionasilobackend.repository.EspecialidadRepository;

/** YS-84. El catálogo de especialidades de la fundación. */
@Service
public class EspecialidadService {

    private final EspecialidadRepository especialidadRepository;

    public EspecialidadService(EspecialidadRepository especialidadRepository) {
        this.especialidadRepository = especialidadRepository;
    }

    /** Solo las activas: una que la fundación dejó de ofrecer no se puede elegir. */
    @Transactional(readOnly = true)
    public List<EspecialidadResponse> listarActivas() {
        return especialidadRepository.findByActivaTrueOrderByNombreAsc().stream()
                .map(especialidad -> new EspecialidadResponse(especialidad.getId(), especialidad.getNombre()))
                .toList();
    }
}
