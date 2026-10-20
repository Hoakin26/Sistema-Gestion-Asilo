package app.cabezadealgodon.com.gestionasilobackend.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.CuentaMensualResumenResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.EstadoCuentaResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.ResumenPersona;
import app.cabezadealgodon.com.gestionasilobackend.exception.CampoInvalidoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.CargoServicio;
import app.cabezadealgodon.com.gestionasilobackend.models.CuentaMensual;
import app.cabezadealgodon.com.gestionasilobackend.models.Interno;
import app.cabezadealgodon.com.gestionasilobackend.models.PoliticaDescuento;
import app.cabezadealgodon.com.gestionasilobackend.models.TipoServicio;
import app.cabezadealgodon.com.gestionasilobackend.repository.CargoServicioRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.CuentaMensualRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.InternoRepository;

/**
 * YS-132. La cuenta del familiar (HU-18, RF-12, CU-006 pasos 1 a 4): reúne
 * cada mes la cuota base del interno y los cargos de consultas, laboratorio y
 * farmacia que nacieron en HU-09, HU-14 y HU-17, y los desglosa por servicio.
 *
 * <p>
 * YS-136. Al consolidar se aplican los descuentos de la fundación (HU-19):
 * cada cargo del mes toma la política vigente en la fecha de su servicio, con
 * la misma {@link CalculadoraDescuento} que lo calculó al nacer. Los pagos y
 * los estados de DE-04 llegan con HU-34 (RF-22), decidido el 20/10.
 */
@Service
public class CuentaFamiliarService {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);

    private final CuentaMensualRepository cuentaRepository;
    private final CargoServicioRepository cargoRepository;
    private final InternoRepository internoRepository;
    private final CalculadoraDescuento calculadora;

    public CuentaFamiliarService(CuentaMensualRepository cuentaRepository, CargoServicioRepository cargoRepository,
            InternoRepository internoRepository, CalculadoraDescuento calculadora) {
        this.cuentaRepository = cuentaRepository;
        this.cargoRepository = cargoRepository;
        this.internoRepository = internoRepository;
        this.calculadora = calculadora;
    }

    /**
     * HU-18. Consolida el mes: una cuenta por interno, a cargo de su familiar
     * responsable, con su cuota base más los cargos del mes. La tienen los
     * internos activos con cuota, aunque no hayan recibido servicios, y
     * cualquiera que haya recibido alguno, aunque ya no tenga cuota.
     *
     * <p>
     * Consolidar dos veces no duplica nada: la cuenta es una por interno y mes,
     * y se recalcula entera con los cargos que tenga en ese momento. Así un
     * cargo que llegó después (un resultado cargado tarde, un despacho
     * pendiente) entra al volver a consolidar, y también una cuota que se fijó
     * después. Mientras no haya pagos (HU-34), recalcular no le quita nada a
     * nadie; HU-34 decidirá qué pasa con una cuenta que ya tiene pagos.
     *
     * <p>
     * YS-136. Antes de sumar, cada cargo vuelve a tomar el descuento de la
     * política vigente en su fecha (HU-19): una política que la fundación
     * definió después, pero que ya regía ese día, entra aquí. Las políticas se
     * leen una sola vez para todo el mes.
     *
     * <p>
     * Un mes que todavía no empieza no se consolida (400). El mes en curso sí:
     * la cuenta queda con lo que va del mes, y se vuelve a consolidar al cerrar.
     */
    @Transactional
    public List<CuentaMensualResumenResponse> consolidar(YearMonth periodo) {
        if (periodo.isAfter(YearMonth.now())) {
            throw new CampoInvalidoException("periodo", "Ese mes todavía no empieza: no hay nada que consolidar");
        }
        String clave = periodo.toString();

        Map<Long, Interno> internos = new LinkedHashMap<>();
        Map<Long, List<CargoServicio>> cargosPorInterno = new HashMap<>();
        List<PoliticaDescuento> politicas = calculadora.politicas();
        for (CargoServicio cargo : cargoRepository.entre(periodo.atDay(1), periodo.atEndOfMonth())) {
            DesgloseCosto desglose = calculadora.calcular(cargo.getTipo(), cargo.getMontoOriginal(), cargo.getFecha(),
                    politicas);
            cargo.setDescuento(desglose.descuento());
            cargo.setMontoNeto(desglose.montoNeto());
            internos.putIfAbsent(cargo.getInterno().getId(), cargo.getInterno());
            cargosPorInterno.computeIfAbsent(cargo.getInterno().getId(), id -> new ArrayList<>()).add(cargo);
        }
        internoRepository.activosConCuota().forEach(interno -> internos.putIfAbsent(interno.getId(), interno));
        // Una cuenta que ya existe se recalcula aunque hoy no le toque ninguna regla de arriba.
        Map<Long, CuentaMensual> existentes = new HashMap<>();
        for (CuentaMensual cuenta : cuentaRepository.delPeriodo(clave)) {
            existentes.put(cuenta.getInterno().getId(), cuenta);
            internos.putIfAbsent(cuenta.getInterno().getId(), cuenta.getInterno());
        }

        LocalDateTime ahora = LocalDateTime.now();
        List<CuentaMensual> consolidadas = new ArrayList<>();
        for (Interno interno : internos.values()) {
            CuentaMensual cuenta = existentes.get(interno.getId());
            if (cuenta == null) {
                cuenta = new CuentaMensual();
                cuenta.setInterno(interno);
                cuenta.setPeriodo(clave);
            }
            List<CargoServicio> cargos = cargosPorInterno.getOrDefault(interno.getId(), List.of());
            calcular(cuenta, interno, cargos, ahora);
            CuentaMensual guardada = cuentaRepository.save(cuenta);
            cargos.forEach(cargo -> cargo.setCuenta(guardada));
            consolidadas.add(guardada);
        }
        consolidadas.sort(Comparator.comparing((CuentaMensual cuenta) -> cuenta.getInterno().getNombreCompleto())
                .thenComparing(CuentaMensual::getId));
        return consolidadas.stream().map(CuentaFamiliarService::resumen).toList();
    }

    /** HU-18. Las cuentas ya consolidadas de un mes, por nombre del interno. */
    @Transactional(readOnly = true)
    public List<CuentaMensualResumenResponse> cuentas(YearMonth periodo) {
        return cuentaRepository.delPeriodo(periodo.toString()).stream().map(CuentaFamiliarService::resumen).toList();
    }

    /**
     * HU-18. El estado de cuenta de un interno entre dos fechas, desglosado
     * por servicio: la cuota de cada mes consolidado del rango y cada cargo de
     * esas fechas, consolidado o no. Al lado, lo que el familiar debe en total.
     */
    @Transactional(readOnly = true)
    public EstadoCuentaResponse estadoCuenta(Long internoId, LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new CampoInvalidoException("hasta", "La fecha final no puede ser anterior a la inicial");
        }
        if (!internoRepository.existsById(internoId)) {
            throw new RecursoNoEncontradoException("No existe un interno con id " + internoId);
        }

        List<CargoServicio> cargos = cargoRepository.delInternoEntre(internoId, desde, hasta);
        List<CuentaMensual> cuentas = cuentaRepository.findByInternoIdAndPeriodoBetweenOrderByPeriodoAsc(internoId,
                YearMonth.from(desde).toString(), YearMonth.from(hasta).toString());

        BigDecimal cuotas = suma(cuentas, CuentaMensual::getCuotaBase);
        int mesesConCuota = (int) cuentas.stream().filter(cuenta -> cuenta.getCuotaBase().signum() > 0).count();
        List<EstadoCuentaResponse.Subtotal> desglose = new ArrayList<>();
        desglose.add(new EstadoCuentaResponse.Subtotal("CUOTA", mesesConCuota, cuotas, CERO, cuotas));
        for (TipoServicio tipo : TipoServicio.values()) {
            List<CargoServicio> delTipo = cargos.stream().filter(cargo -> cargo.getTipo() == tipo).toList();
            desglose.add(new EstadoCuentaResponse.Subtotal(tipo.name(), delTipo.size(),
                    suma(delTipo, CargoServicio::getMontoOriginal), suma(delTipo, CargoServicio::getDescuento),
                    suma(delTipo, CargoServicio::getMontoNeto)));
        }

        BigDecimal saldoTotal = suma(cuentaRepository.findByInternoId(internoId), CuentaMensual::getSaldo);
        int sinConsolidar = (int) cargos.stream().filter(cargo -> cargo.getCuenta() == null).count();
        return new EstadoCuentaResponse(
                internoId,
                desde,
                hasta,
                desglose,
                cargos.stream().map(CuentaFamiliarService::cargo).toList(),
                cuentas.stream().map(CuentaFamiliarService::resumen).toList(),
                cuotas.add(suma(cargos, CargoServicio::getMontoOriginal)),
                suma(cargos, CargoServicio::getDescuento),
                cuotas.add(suma(cargos, CargoServicio::getMontoNeto)),
                saldoTotal,
                sinConsolidar);
    }

    /**
     * Los totales de la cuenta con los cargos del mes. La cuota es la que el
     * interno tiene hoy; la de uno que ya egresó no cambia al reconsolidar: se
     * queda la que tenía su cuenta.
     */
    private static void calcular(CuentaMensual cuenta, Interno interno, List<CargoServicio> cargos,
            LocalDateTime ahora) {
        BigDecimal cuota = interno.isActivo()
                ? Objects.requireNonNullElse(interno.getCuotaMensual(), BigDecimal.ZERO)
                : Objects.requireNonNullElse(cuenta.getCuotaBase(), BigDecimal.ZERO);
        BigDecimal neto = suma(cargos, CargoServicio::getMontoNeto);

        cuenta.setFamiliar(interno.getFamiliarResponsable());
        cuenta.setCuotaBase(cuota.setScale(2));
        cuenta.setTotalServicios(suma(cargos, CargoServicio::getMontoOriginal));
        cuenta.setTotalDescuentos(suma(cargos, CargoServicio::getDescuento));
        cuenta.setTotalAPagar(cuota.add(neto).setScale(2));
        // HU-34 le resta los pagos.
        cuenta.setSaldo(cuenta.getTotalAPagar());
        cuenta.setFechaConsolidacion(ahora);
    }

    private static <T> BigDecimal suma(List<T> elementos, Function<T, BigDecimal> monto) {
        return elementos.stream().map(monto).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }

    private static CuentaMensualResumenResponse resumen(CuentaMensual cuenta) {
        return new CuentaMensualResumenResponse(
                cuenta.getId(),
                new ResumenPersona(cuenta.getInterno().getId(), cuenta.getInterno().getNombreCompleto()),
                new ResumenPersona(cuenta.getFamiliar().getId(), cuenta.getFamiliar().getNombreCompleto()),
                cuenta.getPeriodo(),
                cuenta.getCuotaBase(),
                cuenta.getTotalServicios(),
                cuenta.getTotalDescuentos(),
                cuenta.getTotalAPagar(),
                cuenta.getSaldo(),
                cuenta.getEstado(),
                cuenta.getFechaConsolidacion());
    }

    /** Sin nada clínico: el tipo, la fecha, el número de solicitud y los montos (diseño, 4.9). */
    private static EstadoCuentaResponse.Cargo cargo(CargoServicio cargo) {
        return new EstadoCuentaResponse.Cargo(
                cargo.getId(),
                cargo.getFecha(),
                cargo.getTipo(),
                cargo.getVisita().getCita().getSolicitud().getNumero(),
                cargo.getMontoOriginal(),
                cargo.getDescuento(),
                cargo.getMontoNeto(),
                cargo.getCuenta() == null ? null : cargo.getCuenta().getPeriodo());
    }
}
