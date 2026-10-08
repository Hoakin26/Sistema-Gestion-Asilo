package app.cabezadealgodon.com.gestionasilobackend.services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.RestablecerPasswordRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.UsuarioRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.UsuarioResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.UsuarioUpdateRequest;
import app.cabezadealgodon.com.gestionasilobackend.exception.CorreoDuplicadoException;
import app.cabezadealgodon.com.gestionasilobackend.exception.OperacionNoPermitidaException;
import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;
import app.cabezadealgodon.com.gestionasilobackend.models.Rol;
import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.RolRepository;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;

/** HU-29 (YS-41): alta, edición y activación/desactivación de usuarios. */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(this::mapear).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return mapear(buscarUsuario(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        String correo = normalizarCorreo(request.correo());
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new CorreoDuplicadoException("Ya existe un usuario con el correo " + correo);
        }

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(request.nombreCompleto().trim());
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(buscarRol(request.rol()));
        usuario.setActivo(true);
        /*
         * YS-74. Esta contraseña la escribió el administrador, no su dueño, así
         * que el usuario queda obligado a cambiarla al entrar. Los criterios de
         * YS-72 solo nombran el restablecimiento, pero el alta produce el mismo
         * problema por el camino más transitado: dos personas conociendo la
         * misma credencial.
         */
        usuario.setDebeCambiarPassword(true);
        return mapear(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = buscarUsuario(id);
        String correo = normalizarCorreo(request.correo());
        usuarioRepository.findByCorreo(correo)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new CorreoDuplicadoException("Ya existe un usuario con el correo " + correo);
                });

        usuario.setNombreCompleto(request.nombreCompleto().trim());
        usuario.setCorreo(correo);
        usuario.setRol(buscarRol(request.rol()));
        return mapear(usuarioRepository.save(usuario));
    }

    /**
     * Los usuarios no se eliminan: se desactivan, para no perder la trazabilidad
     * de los registros clínicos y financieros que ya crearon.
     */
    @Transactional
    public UsuarioResponse cambiarEstado(Long id, boolean activo) {
        Usuario usuario = buscarUsuario(id);
        usuario.setActivo(activo);
        return mapear(usuarioRepository.save(usuario));
    }

    /**
     * YS-71: un administrador asigna una contraseña temporal a quien olvidó la
     * suya. No pide la anterior, y por eso no puede usarse sobre uno mismo:
     * quien encontrara una sesión abierta se apropiaría de esa cuenta sin
     * conocer su contraseña. Para la propia está el cambio de YS-69, que sí la
     * exige.
     */
    @Transactional
    public UsuarioResponse restablecerPassword(Long id, String correoSolicitante,
            RestablecerPasswordRequest request) {
        Usuario usuario = buscarUsuario(id);

        if (usuario.getCorreo().equals(normalizarCorreo(correoSolicitante))) {
            throw new OperacionNoPermitidaException(
                    "Para cambiar su propia contraseña use el cambio de contraseña, que exige la actual");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        // Igual que en YS-69: truncada a milisegundos, porque PostgreSQL guarda
        // microsegundos y al redondear podría pasar al milisegundo siguiente.
        usuario.setFechaInvalidacionSesiones(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        // YS-74. El caso que originó HU-32: el administrador queda conociendo
        // esta contraseña, así que su dueño tiene que reemplazarla al entrar.
        usuario.setDebeCambiarPassword(true);
        return mapear(usuarioRepository.save(usuario));
    }

    /**
     * Sin normalizar, "Ana@asilo.gt" y "ana@asilo.gt" pasarían como correos
     * distintos y la restricción de unicidad quedaría burlada, aunque en la
     * práctica sean el mismo buzón.
     */
    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase();
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con id " + id));
    }

    private Rol buscarRol(RolNombre nombre) {
        return rolRepository.findByNombre(nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el rol " + nombre));
    }

    private UsuarioResponse mapear(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getCorreo(),
                usuario.getRol().getNombre(),
                usuario.getRol().getDescripcion(),
                usuario.isActivo(),
                usuario.getFechaCreacion());
    }
}
