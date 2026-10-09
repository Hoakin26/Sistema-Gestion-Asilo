package app.cabezadealgodon.com.gestionasilobackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.cabezadealgodon.com.gestionasilobackend.models.MedicoEspecialista;
import app.cabezadealgodon.com.gestionasilobackend.models.RolNombre;
import app.cabezadealgodon.com.gestionasilobackend.models.Usuario;
import jakarta.persistence.LockModeType;

public interface MedicoEspecialistaRepository extends JpaRepository<MedicoEspecialista, Long> {

    boolean existsByUsuarioId(Long usuarioId);

    /** Todos, con lo que la lista muestra, de una sola vez. */
    @EntityGraph(attributePaths = { "usuario", "especialidad", "horarios" })
    @Query("select e from MedicoEspecialista e order by e.usuario.nombreCompleto")
    List<MedicoEspecialista> todos();

    /** Los que pueden recibir citas de una especialidad. */
    @EntityGraph(attributePaths = { "usuario", "especialidad", "horarios" })
    @Query("""
            select e from MedicoEspecialista e
            where e.especialidad.id = :especialidadId and e.activo = true
            order by e.usuario.nombreCompleto
            """)
    List<MedicoEspecialista> activosDeEspecialidad(@Param("especialidadId") Long especialidadId);

    /**
     * YS-167. Las cuentas de médico especialista activas que todavía no
     * tienen perfil: a quiénes se les puede registrar uno.
     */
    @Query("""
            select u from Usuario u
            where u.rol.nombre = :rol and u.activo = true
              and not exists (select e from MedicoEspecialista e where e.usuario = u)
            order by u.nombreCompleto
            """)
    List<Usuario> usuariosSinPerfil(@Param("rol") RolNombre rol);

    /**
     * YS-91. Lee el especialista bloqueándolo hasta que termine la
     * transacción. Dos asignaciones de cita para el mismo especialista se
     * hacen una después de la otra: la segunda ya ve la cita de la primera y
     * no puede tomar la misma hora.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from MedicoEspecialista e where e.id = :id")
    Optional<MedicoEspecialista> bloquear(@Param("id") Long id);
}
