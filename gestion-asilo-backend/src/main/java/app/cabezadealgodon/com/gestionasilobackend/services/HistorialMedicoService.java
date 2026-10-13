package app.cabezadealgodon.com.gestionasilobackend.services;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.EspecialidadResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.HistorialMedicoResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ResumenPersona;
import app.cabezadealgodon.com.gestionasilobackend.models.Especialidad;
import app.cabezadealgodon.com.gestionasilobackend.models.Receta;
import app.cabezadealgodon.com.gestionasilobackend.models.SolicitudReferencia;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.models.VisitaMedica;
import app.cabezadealgodon.com.gestionasilobackend.repository.RecetaRepository;
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
    private final RecetaRepository recetaRepository;

    public HistorialMedicoService(VisitaMedicaRepository visitaRepository, RecetaRepository recetaRepository) {
        this.visitaRepository = visitaRepository;
        this.recetaRepository = recetaRepository;
    }

    /**
     * Los antecedentes del interno sin la visita en curso, que es la que el
     * especialista está registrando y no un antecedente: ni ella ni su receta.
     * Sin visita que excluir ({@code null}), todo.
     */
    @Transactional(readOnly = true)
    public HistorialMedicoResponse de(Long internoId, Long visitaEnCurso) {
        List<HistorialMedicoResponse.VisitaAnterior> visitas = visitaRepository.delInterno(internoId).stream()
                .filter(visita -> !Objects.equals(visita.getId(), visitaEnCurso))
                .map(this::anterior)
                .toList();
        // YS-108. Las recetas de HU-10, uno de los criterios de HU-08.
        List<HistorialMedicoResponse.RecetaAnterior> recetas = recetaRepository.delInterno(internoId).stream()
                .filter(receta -> !Objects.equals(receta.getVisita().getId(), visitaEnCurso))
                .map(this::anterior)
                .toList();
        return new HistorialMedicoResponse(internoId, visitas, recetas);
    }

    private HistorialMedicoResponse.VisitaAnterior anterior(VisitaMedica visita) {
        SolicitudReferencia solicitud = visita.getCita().getSolicitud();
        return new HistorialMedicoResponse.VisitaAnterior(
                visita.getId(),
                visita.getEstado(),
                visita.getFechaInicio(),
                visita.getFechaCierre(),
                solicitud.getNumero(),
                especialidad(solicitud.getEspecialidad()),
                persona(visita.getEspecialista().getUsuario()),
                solicitud.getMotivo(),
                visita.getDiagnostico(),
                visita.getObservaciones());
    }

    private HistorialMedicoResponse.RecetaAnterior anterior(Receta receta) {
        VisitaMedica visita = receta.getVisita();
        SolicitudReferencia solicitud = visita.getCita().getSolicitud();
        return new HistorialMedicoResponse.RecetaAnterior(
                receta.getId(),
                receta.getFechaEmision(),
                receta.getEstado(),
                solicitud.getNumero(),
                especialidad(solicitud.getEspecialidad()),
                persona(visita.getEspecialista().getUsuario()),
                receta.getIndicacionesGenerales(),
                MapeoReceta.medicamentos(receta));
    }

    private static EspecialidadResponse especialidad(Especialidad especialidad) {
        return new EspecialidadResponse(especialidad.getId(), especialidad.getNombre());
    }

    private static ResumenPersona persona(Usuario usuario) {
        return new ResumenPersona(usuario.getId(), usuario.getNombreCompleto());
    }
}
