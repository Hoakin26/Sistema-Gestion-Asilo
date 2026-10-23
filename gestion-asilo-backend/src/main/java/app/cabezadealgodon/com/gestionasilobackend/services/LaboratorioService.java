package app.cabezadealgodon.com.gestionasilobackend.services;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import app.cabezadealgodon.com.gestionasilobackend.dto.ArchivoDescargable;
import app.cabezadealgodon.com.gestionasilobackend.dto.CostoOrdenResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.OrdenLaboratorioResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.CampoInvalidoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.OperacionNoPermitidaException;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoOrden;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoVisita;
import app.cabezadealgodon.com.gestionasilobackend.models.OrdenLaboratorio;
import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoArchivoResultado;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.models.VisitaMedica;
import app.cabezadealgodon.com.gestionasilobackend.repository.OrdenLaboratorioRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.VisitaMedicaRepository;

/**
 * YS-114. El trabajo del laboratorio sobre las órdenes que dan los
 * especialistas (RF-09). HU-12 es la bandeja y la toma de cada orden; HU-13 y
 * HU-14 le agregan el registro del resultado y su costo.
 */
@Service
public class LaboratorioService {

    /** YS-117. El largo del resultado en texto; un informe más largo va en el archivo. */
    static final int LARGO_MAXIMO_RESULTADO = 4000;

    private final OrdenLaboratorioRepository ordenRepository;
    private final VisitaMedicaRepository visitaRepository;
    private final AlmacenArchivos almacen;
    private final CargoService cargoService;
    private final DataSize tamanoMaximo;

    public LaboratorioService(OrdenLaboratorioRepository ordenRepository, VisitaMedicaRepository visitaRepository,
            AlmacenArchivos almacen, CargoService cargoService,
            @Value("${spring.servlet.multipart.max-file-size:10MB}") String tamanoMaximo) {
        this.ordenRepository = ordenRepository;
        this.visitaRepository = visitaRepository;
        this.almacen = almacen;
        this.cargoService = cargoService;
        this.tamanoMaximo = DataSize.parse(tamanoMaximo);
    }

    /**
     * HU-12. Las órdenes en alguno de {@code estados} (sin ninguno, todas),
     * filtradas por el nombre o el DPI del paciente.
     *
     * <p>
     * Las órdenes de una misma visita comparten la fecha, así que se desempata
     * por id. Sin eso, el orden entre ellas lo elige la base en cada consulta,
     * y al pasar de página una orden podía repetirse y otra no aparecer nunca.
     */
    @Transactional(readOnly = true)
    public Page<OrdenLaboratorioResponse> ordenes(Set<EstadoOrden> estados, String paciente, Pageable paginado) {
        Set<EstadoOrden> filtro = estados == null || estados.isEmpty() ? EnumSet.allOf(EstadoOrden.class) : estados;
        String patron = PatronBusqueda.contiene(PatronBusqueda.normalizado(paciente));
        Pageable estable = PageRequest.of(paginado.getPageNumber(), paginado.getPageSize(),
                paginado.getSort().and(Sort.by("id")));
        return ordenRepository.bandeja(filtro, patron, estable).map(VisitaMedicaService::orden);
    }

    /**
     * DE-02. El laboratorio toma la orden: pasa de Pendiente a En Proceso y
     * queda anotado quién la tomó. Una orden que ya no está pendiente es un
     * conflicto con su estado (409), no un error del cliente.
     *
     * <p>
     * Si dos personas la toman a la vez, la columna {@code version} hace que
     * la segunda falle al guardar en vez de pisar a la primera.
     */
    @Transactional
    public OrdenLaboratorioResponse iniciar(Long ordenId, Usuario laboratorista) {
        OrdenLaboratorio orden = buscar(ordenId);
        orden.cambiarEstado(EstadoOrden.EN_PROCESO);
        orden.setLaboratorista(laboratorista);
        return VisitaMedicaService.orden(ordenRepository.saveAndFlush(orden));
    }

    /**
     * HU-13 (YS-117). Registra el resultado del examen, con un archivo si lo
     * hay, y la orden pasa a Resultados Cargados (DE-02). Desde ese momento lo
     * ve el especialista que lo pidió, en su visita.
     *
     * <p>
     * Se acepta sobre una orden pendiente o en proceso, como dicen las reglas
     * de negocio del diseño; una que ya tiene resultado o está anulada es un
     * conflicto con su estado (409). Si la orden estaba pendiente, quien carga
     * el resultado queda anotado como quien la atendió.
     *
     * <p>
     * El archivo se guarda antes que la orden. Si la orden no se puede guardar
     * (por ejemplo, otra persona cargó el resultado al mismo tiempo), el
     * archivo se borra: no queda ninguno sin orden que lo mencione.
     *
     * <p>
     * HU-14 (YS-120). Con el resultado llega el costo del examen, que registra
     * el laboratorio (RF-08 v1.1): mayor que cero, y se guarda en la orden y
     * como cargo de laboratorio de la visita, que es lo que suma la cuenta del
     * familiar (HU-18). Se cobra una sola vez porque el resultado se carga una
     * sola vez: la segunda carga es un 409 antes de llegar al cargo. Si algo
     * falla, la transacción deshace el cargo junto con la orden.
     */
    @Transactional
    public OrdenLaboratorioResponse registrarResultado(Long ordenId, String resultado, String costo,
            MultipartFile archivo, Usuario laboratorista) {
        String texto = resultado == null ? "" : resultado.strip();
        if (texto.isEmpty()) {
            throw new CampoInvalidoException("resultado", "Escriba el resultado del examen");
        }
        if (texto.length() > LARGO_MAXIMO_RESULTADO) {
            throw new CampoInvalidoException("resultado",
                    "El resultado no puede exceder " + LARGO_MAXIMO_RESULTADO + " caracteres; adjunte el informe completo como archivo");
        }
        // Mayor que cero y como lo escribe una persona; las mismas reglas que la farmacia (YS-129).
        BigDecimal monto = Montos.costo(costo, "costo", "Indique el costo del examen");
        ArchivoValidado adjunto = validar(archivo);

        OrdenLaboratorio orden = buscar(ordenId);
        orden.cambiarEstado(EstadoOrden.RESULTADOS_CARGADOS);
        if (orden.getLaboratorista() == null) {
            orden.setLaboratorista(laboratorista);
        }
        orden.setResultado(texto);
        orden.setFechaResultado(LocalDateTime.now());
        orden.setCosto(monto);

        String guardado = null;
        if (adjunto != null) {
            guardado = almacen.guardar(adjunto.contenido(), adjunto.tipo().extension());
            orden.setRutaArchivo(guardado);
            orden.setNombreArchivo(adjunto.nombre());
            orden.setTipoArchivo(adjunto.tipo());
            orden.setTamanoArchivo((long) adjunto.contenido().length);
        }
        try {
            OrdenLaboratorio guardada = ordenRepository.saveAndFlush(orden);
            cargoService.registrar(guardada.getVisita(), TipoServicio.LABORATORIO, monto);
            return VisitaMedicaService.orden(guardada);
        } catch (RuntimeException e) {
            if (guardado != null) {
                almacen.borrar(guardado);
            }
            throw e;
        }
    }

    /**
     * HU-13 (YS-117). El archivo de un resultado, para descargarlo. Decidido
     * el 19/10: lo ven el Laboratorio y los médicos.
     *
     * <ul>
     * <li>El Laboratorio y el Médico General, el de cualquier orden: uno lo
     * cargó y el otro lo consulta en el historial centralizado (HU-02).</li>
     * <li>Un especialista, el de las órdenes que dio él y, mientras tiene una
     * visita activa con el mismo paciente, el de los antecedentes de ese
     * paciente (como HU-08). Fuera de eso, 403.</li>
     * </ul>
     * Los demás roles ni siquiera llegan aquí ({@code @LaboratorioOMedico}).
     */
    @Transactional(readOnly = true)
    public ArchivoDescargable archivo(Long ordenId, Usuario usuario) {
        OrdenLaboratorio orden = buscar(ordenId);
        if (usuario.getRol().getNombre() == RolNombre.MEDICO_ESPECIALISTA && !puedeVerElEspecialista(orden, usuario)) {
            throw new OperacionNoPermitidaException(
                    "Solo puede ver los resultados de sus pacientes, o los de un paciente con el que tiene una visita activa");
        }
        if (orden.getRutaArchivo() == null) {
            throw new RecursoNoEncontradoException("Este resultado no tiene archivo adjunto");
        }
        return new ArchivoDescargable(almacen.leer(orden.getRutaArchivo()), orden.getNombreArchivo(),
                orden.getTipoArchivo().tipoMime(), orden.getTamanoArchivo());
    }

    /**
     * HU-14 (YS-120). El costo para el formulario del laboratorio: el sugerido
     * mientras la orden espera su resultado, el registrado después.
     */
    @Transactional(readOnly = true)
    public CostoOrdenResponse costo(Long ordenId) {
        OrdenLaboratorio orden = buscar(ordenId);
        return new CostoOrdenResponse(orden.getCosto(), orden.getEstado() == EstadoOrden.RESULTADOS_CARGADOS);
    }

    private boolean puedeVerElEspecialista(OrdenLaboratorio orden, Usuario especialista) {
        VisitaMedica visita = orden.getVisita();
        return visita.getEspecialista().getUsuario().getId().equals(especialista.getId())
                || visitaRepository.existsByInternoIdAndEspecialistaUsuarioIdAndEstado(
                        visita.getInterno().getId(), especialista.getId(), EstadoVisita.ACTIVA);
    }

    private OrdenLaboratorio buscar(Long ordenId) {
        return ordenRepository.findById(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una orden de laboratorio con id " + ordenId));
    }

    private record ArchivoValidado(byte[] contenido, TipoArchivoResultado tipo, String nombre) {
    }

    /**
     * Sin archivo, {@code null}: es opcional ("si aplica", dice HU-13). Con
     * archivo, se comprueban el tamaño y el tipo por su contenido; el error va
     * en el campo {@code archivo}, para que la pantalla lo muestre ahí.
     */
    private ArchivoValidado validar(MultipartFile archivo) {
        if (archivo == null || (archivo.isEmpty() && nombreOriginal(archivo).isEmpty())) {
            return null;
        }
        if (archivo.isEmpty()) {
            throw new CampoInvalidoException("archivo", "El archivo está vacío");
        }
        if (archivo.getSize() > tamanoMaximo.toBytes()) {
            throw new CampoInvalidoException("archivo",
                    "El archivo pesa más de " + tamanoMaximo.toMegabytes() + " MB, el máximo permitido");
        }
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        TipoArchivoResultado tipo = TipoArchivoResultado.detectar(contenido)
                .orElseThrow(() -> new CampoInvalidoException("archivo", "Solo se aceptan archivos PDF, JPG o PNG"));
        String nombre = nombreOriginal(archivo);
        return new ArchivoValidado(contenido, tipo, nombre.isEmpty() ? "resultado." + tipo.extension() : nombre);
    }

    /**
     * El nombre con que llegó el archivo, sin carpetas ni caracteres de
     * control. Solo se muestra y se usa al descargar: nunca decide dónde se
     * guarda (ver {@link AlmacenArchivos}).
     */
    private static String nombreOriginal(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename();
        nombre = nombre.substring(Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\')) + 1);
        nombre = nombre.replaceAll("\\p{Cntrl}", "").strip();
        return nombre.length() > 255 ? nombre.substring(nombre.length() - 255) : nombre;
    }
}
