package app.cabezadealgodon.com.gestionasilobackend.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.EspecialidadResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.EspecialidadTarifaResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.TarifaRequest;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.Especialidad;
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

    /** YS-169. Las activas con su tarifa de consulta, para que la Fundación la defina. */
    @Transactional(readOnly = true)
    public List<EspecialidadTarifaResponse> listarTarifas() {
        return especialidadRepository.findByActivaTrueOrderByNombreAsc().stream()
                .map(this::conTarifa)
                .toList();
    }

    /**
     * YS-169. Cambiar la tarifa no toca las visitas ya iniciadas: cada una
     * copió la suya al empezar.
     */
    @Transactional
    public EspecialidadTarifaResponse actualizarTarifa(Long id, TarifaRequest request) {
        Especialidad especialidad = especialidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una especialidad con id " + id));
        especialidad.setTarifaConsulta(request.tarifaConsulta());
        return conTarifa(especialidadRepository.save(especialidad));
    }

    private EspecialidadTarifaResponse conTarifa(Especialidad especialidad) {
        return new EspecialidadTarifaResponse(especialidad.getId(), especialidad.getNombre(),
                especialidad.getTarifaConsulta());
    }
}
