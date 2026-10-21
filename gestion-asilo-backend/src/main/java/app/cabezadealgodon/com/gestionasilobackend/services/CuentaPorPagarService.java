package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.CuentaPorPagarResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.HistorialPagosFundacionResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.PagoRequest;
import app.cabezadealgodon.com.gestionasilobackend.exception.CampoInvalidoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.ConflictoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.CuentaPorPagar;
import app.cabezadealgodon.com.gestionasilobackend.models.EstadoCuentaPorPagar;
import app.cabezadealgodon.com.gestionasilobackend.models.PagoFundacion;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.CargoServicioRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.CargoServicioRepository.TotalDelMes;
import app.cabezadealgodon.com.gestionasilobackend.repository.CuentaPorPagarRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.PagoFundacionRepository;

/**
 * YS-139. Las cuentas por pagar del asilo a la fundación (HU-20, RF-13): lo
 * que le debe por las consultas, los exámenes y la farmacia de cada mes, y los
 * pagos que le hace.
 *
 * <p>
 * Decidido el 21/10: una deuda por mes y tipo de servicio, y lo adeudado
 * siempre al día. Es la suma del neto de los cargos de ese mes en el momento
 * en que se consulta, con el descuento de la fundación, porque el descuento lo
 * da ella. No hay que consolidar: un examen cargado tarde entra solo, y una
 * deuda ya pagada vuelve a Pendiente si le llega un cargo nuevo. Lo pagado sí
 * se guarda, en {@link CuentaPorPagar}.
 *
 * <p>
 * Si un descuento definido después baja lo adeudado por debajo de lo pagado,
 * el saldo queda en cero y la deuda Pagada; la pantalla muestra lo pagado y lo
 * adeudado, y la diferencia se corrige anulando el pago.
 */
@Service
public class CuentaPorPagarService {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);

    private final CargoServicioRepository cargoRepository;
    private final CuentaPorPagarRepository cuentaRepository;
    private final PagoFundacionRepository pagoRepository;

    public CuentaPorPagarService(CargoServicioRepository cargoRepository, CuentaPorPagarRepository cuentaRepository,
            PagoFundacionRepository pagoRepository) {
        this.cargoRepository = cargoRepository;
        this.cuentaRepository = cuentaRepository;
        this.pagoRepository = pagoRepository;
    }

    /**
     * El histórico (criterio de HU-20): cada mes con servicios, uno por tipo,
     * del más reciente al más antiguo y en el orden de {@link TipoServicio}.
     */
    @Transactional(readOnly = true)
    public List<CuentaPorPagarResponse> historico() {
        Map<String, CuentaPorPagar> conPagos = new HashMap<>();
        cuentaRepository.findAll().forEach(cuenta -> conPagos.put(clave(cuenta.getPeriodo(), cuenta.getTipoServicio()),
                cuenta));
        return cargoRepository.totalesPorMes().stream()
                .map(total -> {
                    String periodo = YearMonth.of(total.getAnio(), total.getMes()).toString();
                    CuentaPorPagar cuenta = conPagos.get(clave(periodo, total.getTipo()));
                    return resumen(periodo, total.getTipo(), Optional.of(total), cuenta);
                })
                .sorted(Comparator.comparing(CuentaPorPagarResponse::periodo).reversed()
                        .thenComparing(CuentaPorPagarResponse::tipoServicio))
                .toList();
    }

    /** Una deuda con sus pagos. Un mes sin servicios de ese tipo responde en cero, sin pagos. */
    @Transactional(readOnly = true)
    public HistorialPagosFundacionResponse historial(YearMonth periodo, TipoServicio tipo) {
        return historial(periodo, tipo, cuentaRepository.findByPeriodoAndTipoServicio(periodo.toString(), tipo)
                .orElse(null));
    }

    /**
     * Registra un pago a la fundación. Mayor que cero lo revisa
     * {@code PagoRequest}; que no pase del saldo, aquí, con lo adeudado de
     * este momento. Sin saldo, nada que pagar (409).
     */
    @Transactional
    public HistorialPagosFundacionResponse registrar(YearMonth periodo, TipoServicio tipo, PagoRequest request,
            Usuario usuario) {
        CuentaPorPagar cuenta = cuentaRepository.findByPeriodoAndTipoServicio(periodo.toString(), tipo)
                .orElseGet(() -> nueva(periodo, tipo));
        BigDecimal adeudado = adeudado(periodo, tipo);
        BigDecimal saldo = saldo(adeudado, cuenta.getTotalPagado());
        if (saldo.signum() == 0) {
            throw new ConflictoException(adeudado.signum() == 0
                    ? "No hay nada que pagarle a la fundación por ese servicio en ese mes"
                    : "Esta deuda ya está pagada completa");
        }
        BigDecimal monto = request.monto().setScale(2);
        if (monto.compareTo(saldo) > 0) {
            throw new CampoInvalidoException("monto",
                    "El pago no puede pasar del saldo pendiente, " + PagoService.quetzales(saldo));
        }

        // La fila primero: el pago la referencia, y nace con el primer pago.
        CuentaPorPagar guardada = cuentaRepository.saveAndFlush(cuenta);
        PagoFundacion pago = new PagoFundacion();
        pago.setCuenta(guardada);
        pago.setMonto(monto);
        pago.setFecha(request.fecha());
        pago.setFormaPago(request.formaPago());
        pago.setReferencia(PagoService.opcional(request.referencia()));
        pago.setRegistradoPor(usuario);
        pagoRepository.save(pago);

        guardada.registrarPago(monto);
        return historial(periodo, tipo, cuentaRepository.saveAndFlush(guardada));
    }

    /**
     * Anula un pago mal registrado, como en la cuenta del familiar (YS-176):
     * queda en el historial con el motivo y su monto vuelve al saldo. Uno ya
     * anulado no se vuelve a anular (409).
     */
    @Transactional
    public HistorialPagosFundacionResponse anular(Long pagoId, String motivo, Usuario usuario) {
        PagoFundacion pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un pago a la fundación con id " + pagoId));
        if (pago.isAnulado()) {
            throw new ConflictoException("Este pago ya está anulado");
        }
        pago.anular(motivo.strip(), usuario, LocalDateTime.now());
        CuentaPorPagar cuenta = pago.getCuenta();
        cuenta.anularPago(pago.getMonto());
        CuentaPorPagar guardada = cuentaRepository.saveAndFlush(cuenta);
        return historial(YearMonth.parse(guardada.getPeriodo()), guardada.getTipoServicio(), guardada);
    }

    private HistorialPagosFundacionResponse historial(YearMonth periodo, TipoServicio tipo, CuentaPorPagar cuenta) {
        CuentaPorPagarResponse resumen = resumen(periodo.toString(), tipo, totalDelMes(periodo, tipo), cuenta);
        if (cuenta == null || cuenta.getId() == null) {
            return new HistorialPagosFundacionResponse(resumen, List.of());
        }
        return new HistorialPagosFundacionResponse(resumen,
                pagoRepository.findByCuentaIdOrderByFechaDescIdDesc(cuenta.getId()).stream()
                        .map(PagoService::respuesta)
                        .toList());
    }

    /** Lo adeudado, lo pagado (de la fila, si ya tiene pagos), el saldo y el estado. */
    private static CuentaPorPagarResponse resumen(String periodo, TipoServicio tipo, Optional<TotalDelMes> total,
            CuentaPorPagar cuenta) {
        long cantidad = total.map(TotalDelMes::getCantidad).orElse(0L);
        BigDecimal adeudado = total.map(TotalDelMes::getTotal).orElse(BigDecimal.ZERO).setScale(2);
        BigDecimal pagado = cuenta == null ? CERO : cuenta.getTotalPagado();
        BigDecimal saldo = saldo(adeudado, pagado);
        return new CuentaPorPagarResponse(periodo, tipo, cantidad, adeudado, pagado, saldo,
                saldo.signum() == 0 ? EstadoCuentaPorPagar.PAGADA : EstadoCuentaPorPagar.PENDIENTE);
    }

    private Optional<TotalDelMes> totalDelMes(YearMonth periodo, TipoServicio tipo) {
        return cargoRepository.totalDelMes(tipo, periodo.atDay(1), periodo.atEndOfMonth());
    }

    private BigDecimal adeudado(YearMonth periodo, TipoServicio tipo) {
        return totalDelMes(periodo, tipo).map(TotalDelMes::getTotal).orElse(BigDecimal.ZERO).setScale(2);
    }

    /** Lo que falta pagar; nunca negativo, aunque un descuento posterior baje lo adeudado. */
    private static BigDecimal saldo(BigDecimal adeudado, BigDecimal pagado) {
        return adeudado.subtract(pagado).max(BigDecimal.ZERO).setScale(2);
    }

    private static CuentaPorPagar nueva(YearMonth periodo, TipoServicio tipo) {
        CuentaPorPagar cuenta = new CuentaPorPagar();
        cuenta.setPeriodo(periodo.toString());
        cuenta.setTipoServicio(tipo);
        return cuenta;
    }

    private static String clave(String periodo, TipoServicio tipo) {
        return periodo + "/" + tipo;
    }
}
