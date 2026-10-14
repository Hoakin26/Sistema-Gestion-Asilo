package app.cabezadealgodon.com.gestionasilobackend.services;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.EspecialidadResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ExamenLaboratorioResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.HistorialMedicoResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ResumenPersona;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.Especialidad;
import app.cabezadealgodon.com.gestionasilobackend.models.OrdenLaboratorio;
import app.cabezadealgodon.com.gestionasilobackend.models.Receta;
import app.cabezadealgodon.com.gestionasilobackend.models.SolicitudReferencia;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.models.VisitaMedica;
import app.cabezadealgodon.com.gestionasilobackend.repository.InternoRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.OrdenLaboratorioRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.RecetaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.VisitaMedicaRepository;

/**
 * YS-102. Arma los antecedentes de un interno a partir de lo que ya está
 * registrado: el historial no se guarda aparte, se consulta (sección 3.4 del
 * diseño), así que nunca puede contradecir a sus fuentes.
 *
 * <p>
 * No decide quién puede verlo: eso lo hace quien lo llama. HU-08 lo abre al
 * especialista durante su visita activa; HU-02 (YS-64), al Médico General.
 */
@Service
public class HistorialMedicoService {

    private final InternoRepository internoRepository;
    private final VisitaMedicaRepository visitaRepository;
    private final RecetaRepository recetaRepository;
    private final OrdenLaboratorioRepository ordenRepository;

    public HistorialMedicoService(InternoRepository internoRepository, VisitaMedicaRepository visitaRepository,
            RecetaRepository recetaRepository, OrdenLaboratorioRepository ordenRepository) {
        this.internoRepository = internoRepository;
        this.visitaRepository = visitaRepository;
        this.recetaRepository = recetaRepository;
        this.ordenRepository = ordenRepository;
    }

    /**
     * HU-02 (YS-64). El historial completo del interno, con la visita que
     * esté en curso incluida: para el Médico General no hay una consulta
     * propia que excluir. Todo llega de la más reciente a la más antigua.
     *
     * <p>
     * Un interno sin visitas responde con las listas vacías; uno que no
     * existe, 404, para que la pantalla no muestre "sin consultas" de alguien
     * que nunca estuvo registrado.
     */
    @Transactional(readOnly = true)
    public HistorialMedicoResponse delInterno(Long internoId) {
        if (!internoRepository.existsById(internoId)) {
            throw new RecursoNoEncontradoException("No existe un interno con id " + internoId);
        }
        return de(internoId, null);
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
        // YS-111. Los exámenes de HU-11, el último criterio de HU-08.
        List<HistorialMedicoResponse.ExamenAnterior> examenes = ordenRepository.delInterno(internoId).stream()
                .filter(orden -> !Objects.equals(orden.getVisita().getId(), visitaEnCurso))
                .map(this::anterior)
                .toList();
        return new HistorialMedicoResponse(internoId, visitas, recetas, examenes);
    }

    private HistorialMedicoResponse.ExamenAnterior anterior(OrdenLaboratorio orden) {
        VisitaMedica visita = orden.getVisita();
        SolicitudReferencia solicitud = visita.getCita().getSolicitud();
        return new HistorialMedicoResponse.ExamenAnterior(
                orden.getId(),
                visita.getId(),
                orden.getFechaOrden(),
                orden.getEstado(),
                new ExamenLaboratorioResponse(orden.getExamen().getId(), orden.getExamen().getNombre()),
                orden.getIndicaciones(),
                solicitud.getNumero(),
                especialidad(solicitud.getEspecialidad()),
                persona(visita.getEspecialista().getUsuario()));
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
                visita.getId(),
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
