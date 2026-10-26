package app.cabezadealgodon.com.gestionasilobackend;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import app.cabezadealgodon.com.gestionasilobackend.exception.DemasiadosIntentosException;
import app.cabezadealgodon.com.gestionasilobackend.security.ControlIntentosLogin;

@DisplayName("Límite de intentos de inicio de sesión (DEF-01)")
class ControlIntentosLoginTest {

    private static final String IP = "10.0.0.1";

    private final RelojDePrueba reloj = new RelojDePrueba(Instant.parse("2026-10-26T15:00:00Z"));
    private final ControlIntentosLogin intentos = new ControlIntentosLogin(reloj, 5, 20, Duration.ofMinutes(15));

    @Test
    @DisplayName("Cinco fallos bloquean el correo 15 minutos; pasada la ventana, se vuelve a entrar")
    void cincoFallosBloqueanElCorreo() {
        fallar("a@asilo.gt", IP, 5);

        assertThatThrownBy(() -> intentos.comprobar("a@asilo.gt", IP))
                .isInstanceOf(DemasiadosIntentosException.class)
                .hasMessage("Demasiados intentos fallidos. Espere 15 minutos e intente de nuevo");
        // Otro correo desde la misma IP sigue entrando.
        assertThatCode(() -> intentos.comprobar("b@asilo.gt", IP)).doesNotThrowAnyException();

        reloj.avanzar(Duration.ofMinutes(15));
        assertThatCode(() -> intentos.comprobar("a@asilo.gt", IP)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Veinte fallos desde una IP la bloquean, aunque cada correo sea distinto")
    void veinteFallosBloqueanLaIp() {
        for (int i = 0; i < 20; i++) {
            intentos.registrarFallo("usuario" + i + "@asilo.gt", "10.0.0.2");
        }

        assertThatThrownBy(() -> intentos.comprobar("nuevo@asilo.gt", "10.0.0.2"))
                .isInstanceOf(DemasiadosIntentosException.class);
        assertThatCode(() -> intentos.comprobar("nuevo@asilo.gt", "10.0.0.3")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Los fallos de hace más de 15 minutos ya no cuentan")
    void losFallosViejosNoCuentan() {
        fallar("a@asilo.gt", IP, 4);
        reloj.avanzar(Duration.ofMinutes(16));
        fallar("a@asilo.gt", IP, 4);

        assertThatCode(() -> intentos.comprobar("a@asilo.gt", IP)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un ingreso correcto borra los fallos de ese correo")
    void ingresarBienReiniciaLaCuenta() {
        fallar("a@asilo.gt", IP, 4);
        intentos.reiniciar("a@asilo.gt");
        fallar("a@asilo.gt", IP, 4);

        assertThatCode(() -> intentos.comprobar("a@asilo.gt", IP)).doesNotThrowAnyException();
    }

    private void fallar(String correo, String ip, int veces) {
        for (int i = 0; i < veces; i++) {
            intentos.comprobar(correo, ip);
            intentos.registrarFallo(correo, ip);
        }
    }

    /** Un reloj que solo avanza cuando la prueba lo pide. */
    static final class RelojDePrueba extends Clock {
        private Instant ahora;

        RelojDePrueba(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(Duration tiempo) {
            ahora = ahora.plus(tiempo);
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }
    }
}