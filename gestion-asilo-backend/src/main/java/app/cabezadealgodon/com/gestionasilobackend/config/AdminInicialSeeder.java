package app.cabezadealgodon.com.gestionasilobackend.config;

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
 */
@Component
@Order(2)
public class AdminInicialSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final String correo;
    private final String password;

    public AdminInicialSeeder(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin-inicial.correo:admin@asilo.gt}") String correo,
            @Value("${app.admin-inicial.password:Admin123*}") String password) {
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

        Usuario admin = new Usuario();
        admin.setNombreCompleto("Administrador del Asilo");
        admin.setCorreo(correo);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRol(rol);
        admin.setActivo(true);
        usuarioRepository.save(admin);

        log.warn("Administrador inicial creado ({}). Cambia su contraseña antes de usar el sistema.", correo);
    }
}
