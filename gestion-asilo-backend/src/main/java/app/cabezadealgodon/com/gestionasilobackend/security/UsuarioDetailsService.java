package app.cabezadealgodon.com.gestionasilobackend.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import app.cabezadealgodon.com.gestionasilobackend.repository.UsuarioRepository;

/**
 * Carga el usuario y su rol desde la base de datos para que Spring Security
 * pueda autenticarlo. El rol se expone con el prefijo ROLE_ porque es lo que
 * espera hasRole()/@PreAuthorize (ver docs/seguridad.md).
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));

        return User.builder()
                .username(usuario.getCorreo())
                .password(usuario.getPasswordHash())
                .disabled(!usuario.isActivo())
                .authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre()))
                .build();
    }
}
