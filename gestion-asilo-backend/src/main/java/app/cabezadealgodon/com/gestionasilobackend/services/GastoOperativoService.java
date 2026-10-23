package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.GastoOperativoRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.GastoOperativoResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.HistoricoGastosResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.CampoInvalidoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.ConflictoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.GastoOperativo;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoGasto;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.GastoOperativoRepository;

/**
 * YS-148. Los gastos operativos del asilo (HU-23, RF-16): su registro por tipo,
 * y su histórico filtrado por tipo y por fechas, con lo que suman. Uno mal
 * registrado se anula con su motivo, como las donaciones (YS-145).
 */
@Service
public class GastoOperativoService {

    /** Sin fechas, todos: desde antes de que existiera el asilo hasta muy adelante. */
    private static final LocalDate SIN_INICIO = LocalDate.of(1900, 1, 1);
    private static final LocalDate SIN_FIN = LocalDate.of(3000, 1, 1);

    private final GastoOperativoRepository gastoRepository;

    public GastoOperativoService(GastoOperativoRepository gastoRepository) {
        this.gastoRepository = gastoRepository;
    }

    /**
     * Registra un gasto. Las reglas de cada campo las revisa
     * {@code GastoOperativoRequest}; aquí, la que depende de dos: con el tipo
     * Otro, la descripción es obligatoria, porque es lo único que dice en qué
     * se gastó.
     */
    @Transactional
    public GastoOperativoResponse registrar(GastoOperativoRequest request, Usuario usuario) {
        String descripcion = PagoService.opcional(request.descripcion());
        if (request.tipoGasto() == TipoGasto.OTRO && descripcion == null) {
            throw new CampoInvalidoException("descripcion", "Con el tipo Otro, describa en qué se gastó");
        }
        GastoOperativo gasto = new GastoOperativo();
        gasto.setTipoGasto(request.tipoGasto());
        gasto.setDescripcion(descripcion);
        gasto.setMonto(request.monto().setScale(2));
        gasto.setFecha(request.fecha());
        gasto.setRegistradoPor(usuario);
        return respuesta(gastoRepository.saveAndFlush(gasto));
    }

    /**
     * El histórico: los gastos de {@code tipos} entre {@code desde} y
     * {@code hasta}, las dos incluidas, del más reciente al más antiguo. Sin
     * tipos, de todos; sin fechas, de cualquier fecha. Los totales no cuentan
     * los anulados.
     */
    @Transactional(readOnly = true)
    public HistoricoGastosResponse historico(LocalDate desde, LocalDate hasta, Collection<TipoGasto> tipos) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new CampoInvalidoException("hasta", "La fecha final no puede ser anterior a la inicial");
        }
        List<GastoOperativo> gastos = gastoRepository.historico(
                desde == null ? SIN_INICIO : desde,
                hasta == null ? SIN_FIN : hasta,
                tipos == null || tipos.isEmpty() ? Arrays.asList(TipoGasto.values()) : tipos);

        List<GastoOperativo> vigentes = gastos.stream().filter(gasto -> !gasto.isAnulado()).toList();
        List<HistoricoGastosResponse.TotalPorTipo> porTipo = Arrays.stream(TipoGasto.values())
                .map(tipo -> {
                    List<GastoOperativo> delTipo = vigentes.stream()
                            .filter(gasto -> gasto.getTipoGasto() == tipo)
                            .toList();
                    return new HistoricoGastosResponse.TotalPorTipo(tipo, delTipo.size(), suma(delTipo));
                })
                .toList();
        return new HistoricoGastosResponse(gastos.stream().map(GastoOperativoService::respuesta).toList(),
                suma(vigentes), porTipo);
    }

    /**
     * Anula un gasto mal registrado. No se borra: queda en el histórico con el
     * motivo, quién lo anuló y cuándo, y deja de contar en los totales. Uno ya
     * anulado no se vuelve a anular (409).
     */
    @Transactional
    public GastoOperativoResponse anular(Long id, String motivo, Usuario usuario) {
        GastoOperativo gasto = gastoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un gasto con id " + id));
        if (gasto.isAnulado()) {
            throw new ConflictoException("Este gasto ya está anulado");
        }
        gasto.anular(motivo.strip(), usuario, LocalDateTime.now());
        return respuesta(gastoRepository.saveAndFlush(gasto));
    }

    private static BigDecimal suma(List<GastoOperativo> gastos) {
        return gastos.stream().map(GastoOperativo::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }

    private static GastoOperativoResponse respuesta(GastoOperativo gasto) {
        return new GastoOperativoResponse(
                gasto.getId(),
                gasto.getTipoGasto(),
                gasto.getDescripcion(),
                gasto.getMonto(),
                gasto.getFecha(),
                gasto.getRegistradoPor().getNombreCompleto(),
                gasto.getFechaRegistro(),
                gasto.isAnulado(),
                gasto.getMotivoAnulacion(),
                gasto.getAnuladoPor() == null ? null : gasto.getAnuladoPor().getNombreCompleto(),
                gasto.getFechaAnulacion());
    }
}
