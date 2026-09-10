package app.cabezadealgodon.com.gestionasilobackend.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import app.cabezadealgodon.com.gestionasilobackend.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Responde 401 con el mismo formato de error que el resto de la API. */
@Component
public class EntryPointNoAutenticado implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public EntryPointNoAutenticado(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest peticion, HttpServletResponse respuesta,
            AuthenticationException excepcion) throws IOException {
        respuesta.setStatus(HttpStatus.UNAUTHORIZED.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");

        ErrorResponse cuerpo = ErrorResponse.de(HttpStatus.UNAUTHORIZED,
                "Debe iniciar sesión para acceder a este recurso", peticion.getRequestURI());
        objectMapper.writeValue(respuesta.getWriter(), cuerpo);
    }
}
