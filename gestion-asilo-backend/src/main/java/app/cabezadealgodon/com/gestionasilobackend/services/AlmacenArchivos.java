package app.cabezadealgodon.com.gestionasilobackend.services;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import app.cabezadealgodon.com.gestionasilobackend.exception.RecursoNoEncontradoException;

/**
 * YS-117. Dónde quedan los archivos de los resultados de laboratorio (HU-13):
 * una carpeta del disco fuera de la aplicación, que en producción es el volumen
 * {@code asilo_archivos} (docs/fase-diseno.md, 2.5) y se respalda con la base.
 *
 * <p>
 * Cada archivo se guarda con un nombre que genera el sistema. El nombre que
 * traía el archivo no se usa para nada del disco: con él, alguien podría
 * escribir {@code ../../} y guardar o leer fuera de la carpeta.
 *
 * <p>
 * La carpeta se crea al guardar el primer archivo y no al arrancar, para que
 * el backend arranque aunque todavía no exista.
 */
@Service
public class AlmacenArchivos {

    private static final Logger log = LoggerFactory.getLogger(AlmacenArchivos.class);

    private final Path carpeta;

    public AlmacenArchivos(@Value("${app.archivos.directorio:${user.home}/.asilo/archivos}") String carpeta) {
        this.carpeta = Path.of(carpeta).toAbsolutePath().normalize();
    }

    /** Guarda el contenido con un nombre nuevo y devuelve ese nombre. */
    public String guardar(byte[] contenido, String extension) {
        String nombre = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(carpeta);
            Files.write(ubicar(nombre), contenido, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo en " + carpeta, e);
        }
        return nombre;
    }

    public Resource leer(String nombre) {
        Path ruta = ubicar(nombre);
        if (!Files.isRegularFile(ruta)) {
            // La orden dice que tiene archivo y el disco no lo tiene: alguien lo
            // borró a mano o el volumen no se restauró con la base.
            log.error("Falta en el almacén el archivo {} de un resultado de laboratorio", nombre);
            throw new RecursoNoEncontradoException("El archivo de este resultado no está disponible");
        }
        return new FileSystemResource(ruta);
    }

    /**
     * Para deshacer un guardado cuando la orden no se pudo actualizar: sin
     * esto quedaría un archivo que ninguna orden menciona.
     */
    public void borrar(String nombre) {
        try {
            Files.deleteIfExists(ubicar(nombre));
        } catch (IOException e) {
            log.warn("No se pudo borrar el archivo huérfano {}", nombre, e);
        }
    }

    /** La ruta dentro de la carpeta; nunca fuera de ella. */
    private Path ubicar(String nombre) {
        Path ruta = carpeta.resolve(nombre).normalize();
        if (!ruta.getParent().equals(carpeta)) {
            throw new IllegalArgumentException("Nombre de archivo inválido: " + nombre);
        }
        return ruta;
    }
}
