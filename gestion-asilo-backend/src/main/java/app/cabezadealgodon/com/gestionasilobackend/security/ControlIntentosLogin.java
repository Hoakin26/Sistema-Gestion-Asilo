package app.cabezadealgodon.com.gestionasilobackend.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import app.cabezadealgodon.com.gestionasilobackend.exception.DemasiadosIntentosException;

/**
 * DEF-01 (PP-07). Cuenta los inicios de sesión fallidos por correo y por IP,
 * y bloquea los que pasan del límite dentro de la ventana. En memoria: el
 * sistema corre en una sola instancia.
 *
 * <p>
 * El correo se cuenta exista o no: si solo se contaran los registrados, el
 * 429 revelaría qué correos existen.
 */
@Component
public class ControlIntentosLogin {

    private static final Logger bitacora = LoggerFactory.getLogger(ControlIntentosLogin.class);

    private final Clock reloj;
    private final int maximoPorCorreo;
    private final int maximoPorIp;
    private final Duration ventana;

    /** Por clave ("correo:..." o "ip:..."), los momentos de sus fallos recientes. */
    private final Map<String, Deque<Instant>> fallos = new ConcurrentHashMap<>();
    /** Por clave, hasta cuándo está bloqueada. */
    private final Map<String, Instant> bloqueos = new ConcurrentHashMap<>();

    @Autowired
    public ControlIntentosLogin(
            @Value("${app.login.max-fallos-correo:5}") int maximoPorCorreo,
            @Value("${app.login.max-fallos-ip:20}") int maximoPorIp,
            @Value("${app.login.ventana-minutos:15}") long ventanaMinutos) {
        this(Clock.systemUTC(), maximoPorCorreo, maximoPorIp, Duration.ofMinutes(ventanaMinutos));
    }

    /** Para las pruebas, con un reloj que se puede adelantar. */
    public ControlIntentosLogin(Clock reloj, int maximoPorCorreo, int maximoPorIp, Duration ventana) {
        this.reloj = reloj;
        this.maximoPorCorreo = maximoPorCorreo;
        this.maximoPorIp = maximoPorIp;
        this.ventana = ventana;
    }

    /** Lanza DemasiadosIntentosException si el correo o la IP están bloqueados. */
    public void comprobar(String correo, String ip) {
        Instant ahora = reloj.instant();
        for (String clave : new String[] { "correo:" + correo, "ip:" + ip }) {
            Instant hasta = bloqueos.get(clave);
            if (hasta == null) {
                continue;
            }
            if (ahora.isBefore(hasta)) {
                throw new DemasiadosIntentosException(Duration.between(ahora, hasta));
            }
            bloqueos.remove(clave, hasta);
        }
    }

    public void registrarFallo(String correo, String ip) {
        contar("correo:" + correo, maximoPorCorreo);
        contar("ip:" + ip, maximoPorIp);
    }

    /** Un ingreso correcto borra los fallos de ese correo. Los de la IP se quedan. */
    public void reiniciar(String correo) {
        fallos.remove("correo:" + correo);
    }

    private void contar(String clave, int maximo) {
        Instant ahora = reloj.instant();
        Instant limite = ahora.minus(ventana);
        fallos.compute(clave, (k, previos) -> {
            Deque<Instant> recientes = previos == null ? new ArrayDeque<>() : previos;
            recientes.addLast(ahora);
            while (recientes.getFirst().isBefore(limite)) {
                recientes.removeFirst();
            }
            if (recientes.size() < maximo) {
                return recientes;
            }
            bloqueos.put(k, ahora.plus(ventana));
            bitacora.warn("Inicio de sesión bloqueado {} minutos para {}: {} intentos fallidos",
                    ventana.toMinutes(), k, maximo);
            return null;
        });
    }

    /** Cada 15 minutos borra lo vencido, para que el mapa no crezca sin límite. */
    @Scheduled(fixedDelayString = "PT15M")
    public void limpiarVencidos() {
        Instant ahora = reloj.instant();
        Instant limite = ahora.minus(ventana);
        bloqueos.values().removeIf(hasta -> !ahora.isBefore(hasta));
        for (String clave : fallos.keySet()) {
            fallos.computeIfPresent(clave, (k, recientes) -> recientes.getLast().isBefore(limite) ? null : recientes);
        }
    }

    /** Para las pruebas: cada una empieza sin fallos ni bloqueos. */
    public void limpiar() {
        fallos.clear();
        bloqueos.clear();
    }
}