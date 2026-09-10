package app.cabezadealgodon.com.gestionasilobackend.services;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.cabezadealgodon.com.gestionasilobackend.dto.UsuarioRequest;
import app.cabezadealgodon.com.gestionasilobackend.dto.UsuarioResponse;
import app.cabezadealgodon.com.gestionasilobackend.dto.UsuarioUpdateRequest;
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
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(request.nombreCompleto());
        usuario.setCorreo(request.correo());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(buscarRol(request.rol()));
        usuario.setActivo(true);
        return mapear(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = buscarUsuario(id);
        usuario.setNombreCompleto(request.nombreCompleto());
        usuario.setCorreo(request.correo());
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
