package app.cabezadealgodon.com.gestionasilobackend.config;

import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import app.cabezadealgodon.com.gestionasilobackend.models.Rol;
import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.RolRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;

/**
 * Crea el primer Administrador del Asilo si no hay ningún usuario. Sin esto el
 * sistema quedaría bloqueado: crear usuarios exige estar autenticado, y no
 * habría con qué autenticarse.
 *
 * YS-69: no hay contraseña por defecto en el código, porque la conocería
 * cualquiera que lea el repositorio. Si no se configura, se genera al azar y
 * se muestra una sola vez en el log.
 */
@Component
@Order(2)
public class AdminInicialSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialSeeder.class);

    // Sin los caracteres que se confunden al copiarlos del log: 0/O y 1/l/I.
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int LARGO_GENERADA = 16;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final String correo;
    private final String password;

    public AdminInicialSeeder(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin-inicial.correo:admin@asilo.gt}") String correo,
            @Value("${app.admin-inicial.password:}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.correo = correo;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        Rol rol = rolRepository.findByNombre(RolNombre.ADMINISTRADOR_ASILO)
                .orElseThrow(() -> new IllegalStateException("Falta el rol ADMINISTRADOR_ASILO"));

        boolean generada = password.isBlank();
        String inicial = generada ? generarPassword() : password;

        Usuario admin = new Usuario();
        admin.setNombreCompleto("Administrador del Asilo");
        admin.setCorreo(correo);
        admin.setPasswordHash(passwordEncoder.encode(inicial));
        admin.setRol(rol);
        admin.setActivo(true);
        /*
         * YS-74. Es el caso más expuesto de los tres: esta contraseña queda
         * escrita en el log de arranque, o en el archivo de configuración si se
         * fijó a mano. Cualquiera con acceso a esos archivos la conoce, y se
         * trata nada menos que de la cuenta de administrador.
         *
         * Si alguien despliega, no la cambia y pierde el log, se recupera
         * borrando el usuario y volviendo a sembrar con
         * app.admin-inicial.password.
         */
        admin.setDebeCambiarPassword(true);
        usuarioRepository.save(admin);

        if (generada) {
            log.warn("Administrador inicial creado ({}) con la contraseña generada: {} "
                    + "-- se muestra solo esta vez. Cámbiela de inmediato con PUT /api/auth/password.",
                    correo, inicial);
        } else {
            log.warn("Administrador inicial creado ({}) con la contraseña de app.admin-inicial.password. "
                    + "Cámbiela de inmediato con PUT /api/auth/password.", correo);
        }
    }

    private static String generarPassword() {
        SecureRandom azar = new SecureRandom();
        StringBuilder generada = new StringBuilder(LARGO_GENERADA);
        for (int i = 0; i < LARGO_GENERADA; i++) {
            generada.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
        }
        return generada.toString();
    }
}
