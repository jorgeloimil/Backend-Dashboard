package com.homedashboard.exceptions;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

/** Todos los errores salen como ProblemDetail (RFC 7807). */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidacion(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        log.warn("Errores de validación: {}", errores);
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Error de validación", "Datos de entrada inválidos");
        problema.setProperty("errors", errores);
        return problema;
    }

    /** Parámetros de query o de ruta fuera de rango (@Min/@Max del contrato). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleRestricciones(ConstraintViolationException ex) {
        List<String> errores = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();
        log.warn("Parámetros inválidos: {}", errores);
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Error de validación", "Parámetros inválidos");
        problema.setProperty("errors", errores);
        return problema;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleValidacionParametros(HandlerMethodValidationException ex) {
        List<String> errores = ex.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .toList();
        log.warn("Parámetros inválidos: {}", errores);
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Error de validación", "Parámetros inválidos");
        problema.setProperty("errors", errores);
        return problema;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail handlePeticionIlegible(Exception ex) {
        log.warn("Petición ilegible: {}", ex.getMessage());
        return problema(HttpStatus.BAD_REQUEST, "Petición inválida", "El cuerpo o los parámetros no tienen el formato esperado");
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail handleNoEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "No encontrado", ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleRutaInexistente(NoResourceFoundException ex) {
        return problema(HttpStatus.NOT_FOUND, "No encontrado", "La ruta no existe");
    }

    @ExceptionHandler(ServicioExternoException.class)
    public ProblemDetail handleServicioExterno(ServicioExternoException ex) {
        log.warn("Servicio externo caído: {}", ex.getMessage(), ex.getCause());
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Servicio no disponible", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenerico(Exception ex) {
        log.error("Error inesperado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Ha ocurrido un error inesperado");
    }

    private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setProperty("timestamp", Instant.now());
        return problema;
    }
}
