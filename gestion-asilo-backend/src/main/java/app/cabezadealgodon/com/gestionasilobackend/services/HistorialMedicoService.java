package app.cabezadealgodon.com.gestionasilobackend.services;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.EspecialidadResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.HistorialMedicoResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ResumenPersona;
import app.cabezadealgodon.com.gestionasilobackend.models.SolicitudReferencia;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.models.VisitaMedica;
import app.cabezadealgodon.com.gestionasilobackend.repository.VisitaMedicaRepository;

/**
 * YS-102. Arma los antecedentes de un interno a partir de lo que ya está
 * registrado: el historial no se guarda aparte, se consulta (sección 3.4 del
 * diseño), así que nunca puede contradecir a sus fuentes.
 *
 * <p>
 * No decide quién puede verlo: eso lo hace quien lo llama. HU-08 lo abre al
 * especialista durante su visita activa; HU-02 lo abrirá al Médico General.
 */
@Service
public class HistorialMedicoService {

    private final VisitaMedicaRepository visitaRepository;

    public HistorialMedicoService(VisitaMedicaRepository visitaRepository) {
        this.visitaRepository = visitaRepository;
    }

    /**
     * Los antecedentes del interno sin la visita en curso, que es la que el
     * especialista está registrando y no un antecedente. Sin visita que
     * excluir ({@code null}), todos.
     */
    @Transactional(readOnly = true)
    public HistorialMedicoResponse de(Long internoId, Long visitaEnCurso) {
        List<HistorialMedicoResponse.VisitaAnterior> visitas = visitaRepository.delInterno(internoId).stream()
                .filter(visita -> !Objects.equals(visita.getId(), visitaEnCurso))
                .map(this::anterior)
                .toList();
        return new HistorialMedicoResponse(internoId, visitas);
    }

    private HistorialMedicoResponse.VisitaAnterior anterior(VisitaMedica visita) {
        SolicitudReferencia solicitud = visita.getCita().getSolicitud();
        Usuario especialista = visita.getEspecialista().getUsuario();
        return new HistorialMedicoResponse.VisitaAnterior(
                visita.getId(),
                visita.getEstado(),
                visita.getFechaInicio(),
                visita.getFechaCierre(),
                solicitud.getNumero(),
                new EspecialidadResponse(solicitud.getEspecialidad().getId(), solicitud.getEspecialidad().getNombre()),
                new ResumenPersona(especialista.getId(), especialista.getNombreCompleto()),
                solicitud.getMotivo(),
                visita.getDiagnostico(),
                visita.getObservaciones());
    }
}
