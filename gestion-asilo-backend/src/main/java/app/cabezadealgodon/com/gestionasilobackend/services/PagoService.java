package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.HistorialPagosResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.PagoRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.PagoResponse;
import app.cabezadealgodon.com.gestionasilobackend.exception.CampoInvalidoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.ConflictoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.CuentaMensual;
import app.cabezadealgodon.com.gestionasilobackend.models.Pago;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.CuentaMensualRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.PagoRepository;

/**
 * YS-133. Los pagos del familiar (HU-34, RF-22, CU-006 paso 6): se aplican a
 * la cuenta mensual entera, cuota del asilo más servicios con descuento. Un
 * pago parcial se descuenta del saldo y la cuenta queda en Pago Parcial; el
 * pago del total la deja Completamente Cancelada (DE-04).
 *
 * <p>
 * Dos pagos simultáneos sobre la misma cuenta no se pisan: la cuenta tiene
 * {@code version}, y el segundo recibe 409 en vez de pasarse del saldo.
 */
@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final CuentaMensualRepository cuentaRepository;

    public PagoService(PagoRepository pagoRepository, CuentaMensualRepository cuentaRepository) {
        this.pagoRepository = pagoRepository;
        this.cuentaRepository = cuentaRepository;
    }

    /**
     * Registra un pago. Mayor que cero lo revisa {@code PagoRequest}; que no
     * pase del saldo, aquí (criterio de HU-34). Una cuenta ya pagada completa
     * no recibe más pagos (409).
     */
    @Transactional
    public HistorialPagosResponse registrar(Long cuentaId, PagoRequest request, Usuario usuario) {
        CuentaMensual cuenta = buscarCuenta(cuentaId);
        if (cuenta.getSaldo().signum() == 0) {
            throw new ConflictoException("Esta cuenta ya está pagada completa");
        }
        BigDecimal monto = request.monto().setScale(2);
        if (monto.compareTo(cuenta.getSaldo()) > 0) {
            throw new CampoInvalidoException("monto",
                    "El pago no puede pasar del saldo pendiente, " + quetzales(cuenta.getSaldo()));
        }

        Pago pago = new Pago();
        pago.setCuenta(cuenta);
        pago.setMonto(monto);
        pago.setFecha(request.fecha());
        pago.setFormaPago(request.formaPago());
        pago.setReferencia(opcional(request.referencia()));
        pago.setRegistradoPor(usuario);
        pagoRepository.save(pago);

        cuenta.registrarPago(monto, LocalDate.now());
        return historial(cuentaRepository.saveAndFlush(cuenta));
    }

    /** El historial de una cuenta, con su saldo y su estado de hoy. */
    @Transactional(readOnly = true)
    public HistorialPagosResponse historial(Long cuentaId) {
        return historial(buscarCuenta(cuentaId));
    }

    /**
     * YS-176. Anula un pago mal registrado. No se borra: queda en el historial
     * con el motivo, quién lo anuló y cuándo, y su monto vuelve al saldo; el
     * estado se recalcula sin él. Uno ya anulado no se vuelve a anular (409).
     */
    @Transactional
    public HistorialPagosResponse anular(Long pagoId, String motivo, Usuario usuario) {
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un pago con id " + pagoId));
        if (pago.isAnulado()) {
            throw new ConflictoException("Este pago ya está anulado");
        }
        pago.anular(motivo.strip(), usuario, LocalDateTime.now());
        CuentaMensual cuenta = pago.getCuenta();
        cuenta.anularPago(pago.getMonto(), LocalDate.now());
        return historial(cuentaRepository.saveAndFlush(cuenta));
    }

    private HistorialPagosResponse historial(CuentaMensual cuenta) {
        return new HistorialPagosResponse(CuentaFamiliarService.resumen(cuenta),
                pagoRepository.findByCuentaIdOrderByFechaDescIdDesc(cuenta.getId()).stream()
                        .map(PagoService::respuesta)
                        .toList());
    }

    private CuentaMensual buscarCuenta(Long cuentaId) {
        return cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una cuenta mensual con id " + cuentaId));
    }

    private static PagoResponse respuesta(Pago pago) {
        return new PagoResponse(
                pago.getId(),
                pago.getMonto(),
                pago.getFecha(),
                pago.getFormaPago(),
                pago.getReferencia(),
                pago.getRegistradoPor().getNombreCompleto(),
                pago.getFechaRegistro(),
                pago.isAnulado(),
                pago.getMotivoAnulacion(),
                pago.getAnuladoPor() == null ? null : pago.getAnuladoPor().getNombreCompleto(),
                pago.getFechaAnulacion());
    }

    /** "Q2,176.30", como lo lee el Administrador del Asilo en pantalla. */
    private static String quetzales(BigDecimal monto) {
        return String.format(Locale.US, "Q%,.2f", monto);
    }

    private static String opcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.strip();
    }
}
