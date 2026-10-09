package app.cabezadealgodon.com.gestionasilobackend.services;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.FamiliarRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.FamiliarResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.FichaMedicaRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.FichaMedicaResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.InternoRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.InternoResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.InternoResumenResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.MedicamentoHabitualResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.PsicopatologiaResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.DpiDuplicadoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.Familiar;
import app.cabezadealgodon.com.gestionasilobackend.models.FichaMedica;
import app.cabezadealgodon.com.gestionasilobackend.models.Interno;
import app.cabezadealgodon.com.gestionasilobackend.models.MedicamentoHabitual;
import app.cabezadealgodon.com.gestionasilobackend.models.Psicopatologia;
import app.cabezadealgodon.com.gestionasilobackend.repository.FamiliarRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.FichaMedicaRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.InternoRepository;

/** HU-01 (YS-58): alta, edición y consulta del expediente de un interno. */
@Service
public class InternoService {

    private final InternoRepository internoRepository;
    private final FamiliarRepository familiarRepository;
    private final FichaMedicaRepository fichaMedicaRepository;

    public InternoService(InternoRepository internoRepository, FamiliarRepository familiarRepository,
            FichaMedicaRepository fichaMedicaRepository) {
        this.internoRepository = internoRepository;
        this.familiarRepository = familiarRepository;
        this.fichaMedicaRepository = fichaMedicaRepository;
    }

    @Transactional(readOnly = true)
    public Page<InternoResumenResponse> listar(String busqueda, Pageable paginado) {
        String termino = busqueda == null ? "" : busqueda.trim();

        // Igual que el nombre guardado tras el translate del repositorio: sin
        // tildes y en minúsculas, para comparar los dos en la misma forma.
        String nombre = Normalizer.normalize(termino, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);

        // La pantalla muestra el DPI como "1234 56789 0101" y así lo van a
        // copiar; en la base está guardado sin espacios.
        String dpi = termino.matches("[\\d\\s]+") ? termino.replaceAll("\\s", "") : termino;

        return internoRepository.buscar(contiene(nombre), contiene(dpi), paginado).map(this::resumir);
    }

    /**
     * El patrón LIKE de "contiene". Los comodines que escriba el usuario se
     * escapan para que cuenten como texto: buscar "%" no debe traer a todos.
     */
    private static String contiene(String texto) {
        String escapado = texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escapado + "%";
    }

    @Transactional(readOnly = true)
    public InternoResponse obtener(Long id) {
        return mapear(buscarInterno(id));
    }

    /**
     * Registra al interno junto con su ficha médica vacía. Se crean a la vez
     * para que la ficha siempre exista: así consultarla nunca responde 404 y el
     * Médico General solo tiene que completarla, no crearla primero.
     */
    @Transactional
    public InternoResponse registrar(InternoRequest request) {
        if (internoRepository.existsByDpi(request.dpi())) {
            throw new DpiDuplicadoException("Ya existe un interno registrado con el DPI " + request.dpi());
        }

        Interno interno = new Interno();
        aplicar(request, interno);
        interno.setActivo(true);
        interno.setFamiliarResponsable(familiarRepository.save(nuevoFamiliar(request.familiarResponsable())));
        internoRepository.save(interno);

        FichaMedica ficha = new FichaMedica();
        ficha.setInterno(interno);
        fichaMedicaRepository.save(ficha);

        return mapear(interno);
    }

    /**
     * Actualiza los datos personales y los del familiar. No toca la ficha
     * médica: esa se escribe por {@link #actualizarFicha}, reservado al Médico
     * General, y tener un solo camino hacia cada dato evita que dos reglas
     * distintas terminen contradiciéndose.
     */
    @Transactional
    public InternoResponse actualizar(Long id, InternoRequest request) {
        Interno interno = buscarInterno(id);

        // Se compara contra el propio: cambiar cualquier otro campo sin tocar
        // el DPI no puede fallar por "duplicado" contra sí mismo.
        if (!interno.getDpi().equals(request.dpi()) && internoRepository.existsByDpi(request.dpi())) {
            throw new DpiDuplicadoException("Ya existe un interno registrado con el DPI " + request.dpi());
        }

        aplicar(request, interno);
        aplicarFamiliar(request.familiarResponsable(), interno.getFamiliarResponsable());
        return mapear(internoRepository.save(interno));
    }

    /**
     * Los datos clínicos. El control de quién puede llamarlo está en el
     * controlador con {@code @PersonalClinico} (RNF-03); aquí se asume que ya
     * pasó ese filtro.
     */
    @Transactional(readOnly = true)
    public FichaMedicaResponse obtenerFicha(Long internoId) {
        return mapear(buscarFicha(internoId));
    }

    @Transactional
    public FichaMedicaResponse actualizarFicha(Long internoId, FichaMedicaRequest request) {
        FichaMedica ficha = buscarFicha(internoId);
        ficha.setTipoSangre(request.tipoSangre());
        ficha.setAlergias(request.alergias());
        ficha.setMotivoIngreso(request.motivoIngreso());

        /*
         * Se reemplazan las listas completas en vez de calcular diferencias.
         * `clear()` sobre la colección, con orphanRemoval, borra las filas que
         * ya no vienen; reasignar la lista con `setPsicopatologias` en cambio
         * rompería el seguimiento de Hibernate y dejaría huérfanas las viejas.
         */
        ficha.getPsicopatologias().clear();
        if (request.psicopatologias() != null) {
            request.psicopatologias().forEach(entrada -> {
                Psicopatologia psicopatologia = new Psicopatologia();
                psicopatologia.setNombre(entrada.nombre().trim());
                psicopatologia.setFechaDiagnostico(entrada.fechaDiagnostico());
                psicopatologia.setObservaciones(entrada.observaciones());
                ficha.agregarPsicopatologia(psicopatologia);
            });
        }

        ficha.getMedicamentosHabituales().clear();
        if (request.medicamentosHabituales() != null) {
            request.medicamentosHabituales().forEach(entrada -> {
                MedicamentoHabitual medicamento = new MedicamentoHabitual();
                medicamento.setNombre(entrada.nombre().trim());
                medicamento.setDosis(entrada.dosis().trim());
                medicamento.setFrecuencia(entrada.frecuencia().trim());
                ficha.agregarMedicamento(medicamento);
            });
        }

        return mapear(fichaMedicaRepository.save(ficha));
    }

    // ---- apoyo ----

    private void aplicar(InternoRequest request, Interno interno) {
        interno.setNombreCompleto(request.nombreCompleto().trim());
        interno.setDpi(request.dpi().trim());
        interno.setFechaNacimiento(request.fechaNacimiento());
        interno.setFechaIngreso(request.fechaIngreso());
        interno.setCuotaMensual(request.cuotaMensual());
    }

    private Familiar nuevoFamiliar(FamiliarRequest request) {
        Familiar familiar = new Familiar();
        aplicarFamiliar(request, familiar);
        return familiar;
    }

    private void aplicarFamiliar(FamiliarRequest request, Familiar familiar) {
        familiar.setNombreCompleto(request.nombreCompleto().trim());
        familiar.setParentesco(request.parentesco().trim());
        familiar.setTelefono(request.telefono().trim());
        familiar.setCorreo(request.correo());
        familiar.setDireccion(request.direccion());
    }

    private Interno buscarInterno(Long id) {
        return internoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un interno con id " + id));
    }

    private FichaMedica buscarFicha(Long internoId) {
        return fichaMedicaRepository.findByInternoId(internoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la ficha médica del interno con id " + internoId));
    }

    private InternoResumenResponse resumir(Interno interno) {
        return new InternoResumenResponse(interno.getId(), interno.getNombreCompleto(), interno.getDpi(),
                interno.getFechaIngreso(), interno.isActivo());
    }

    private InternoResponse mapear(Interno interno) {
        Familiar familiar = interno.getFamiliarResponsable();
        return new InternoResponse(
                interno.getId(),
                interno.getNombreCompleto(),
                interno.getDpi(),
                interno.getFechaNacimiento(),
                interno.getFechaIngreso(),
                interno.getCuotaMensual(),
                interno.isActivo(),
                new FamiliarResponse(familiar.getId(), familiar.getNombreCompleto(), familiar.getParentesco(),
                        familiar.getTelefono(), familiar.getCorreo(), familiar.getDireccion()),
                interno.getFechaCreacion());
    }

    /**
     * El mapeo se hace dentro de la transacción de quien llama: las dos listas
     * son perezosas y fuera de ella lanzarían LazyInitializationException, como
     * descubrió la prueba de YS-57.
     */
    private FichaMedicaResponse mapear(FichaMedica ficha) {
        List<PsicopatologiaResponse> psicopatologias = ficha.getPsicopatologias().stream()
                .map(p -> new PsicopatologiaResponse(p.getId(), p.getNombre(), p.getFechaDiagnostico(),
                        p.getObservaciones()))
                .toList();

        List<MedicamentoHabitualResponse> medicamentos = ficha.getMedicamentosHabituales().stream()
                .map(m -> new MedicamentoHabitualResponse(m.getId(), m.getNombre(), m.getDosis(), m.getFrecuencia()))
                .toList();

        return new FichaMedicaResponse(
                ficha.getId(),
                ficha.getInterno().getId(),
                ficha.getInterno().getNombreCompleto(),
                ficha.getTipoSangre(),
                ficha.getAlergias(),
                ficha.getMotivoIngreso(),
                psicopatologias,
                medicamentos);
    }
}
