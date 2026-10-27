package dev.jamal.projetotcc.Exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import java.sql.SQLException;
import org.springframework.core.NestedExceptionUtils;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse error = new ApiErrorResponse(
                java.time.LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(
            BusinessException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse error = new ApiErrorResponse(
                java.time.LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        Throwable causa = ex;

        while (causa != null) {

            if (causa instanceof SQLException sqlException) {

                if ("23505".equals(sqlException.getSQLState())) {

                    String mensagem =
                            "Já existe um registro com esses dados.";

                    return ResponseEntity
                            .status(HttpStatus.CONFLICT)
                            .body(new ApiErrorResponse(
                                    LocalDateTime.now(),
                                    HttpStatus.CONFLICT.value(),
                                    mensagem,
                                    request.getRequestURI()
                            ));
                }
            }

            causa = causa.getCause();
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(
                        LocalDateTime.now(),
                        HttpStatus.CONFLICT.value(),
                        "Não foi possível concluir a operação devido a uma restrição de integridade dos dados.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> authentication(HttpServletRequest request) {
        return safe(HttpStatus.UNAUTHORIZED, "Credenciais inválidas ou sessão expirada.", request);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> forbidden(HttpServletRequest request) {
        return safe(HttpStatus.FORBIDDEN, "Acesso não autorizado.", request);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiErrorResponse> rateLimit(HttpServletRequest request) {
        return ResponseEntity.status(429).header("Retry-After", "300")
                .body(new ApiErrorResponse(LocalDateTime.now(), 429, "Muitas solicitações. Tente novamente mais tarde.", request.getRequestURI()));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> status(org.springframework.web.server.ResponseStatusException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatusCode()).body(new ApiErrorResponse(LocalDateTime.now(),
                ex.getStatusCode().value(), ex.getStatusCode().is5xxServerError() ? "Serviço temporariamente indisponível." : "Solicitação não permitida.", request.getRequestURI()));
    }

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiErrorResponse> malformed(HttpServletRequest request) {
        return safe(HttpStatus.BAD_REQUEST, "Dados inválidos na solicitação.", request);
    }

    private ResponseEntity<ApiErrorResponse> safe(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(LocalDateTime.now(), status.value(), message, request.getRequestURI()));
    }

    @ExceptionHandler({org.springframework.dao.OptimisticLockingFailureException.class,
            jakarta.persistence.OptimisticLockException.class})
    public ResponseEntity<ApiErrorResponse> optimisticConflict(HttpServletRequest request) {
        return safe(HttpStatus.CONFLICT, "Os dados foram alterados por outra operação. Entre novamente e tente outra vez.", request);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntime(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse error = new ApiErrorResponse(
                java.time.LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Não foi possível concluir a operação.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String mensagem = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Erro de validação.");

        ApiErrorResponse error = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                mensagem,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(AIProviderException.class)
    public ResponseEntity<Map<String, Object>> handleAIProviderException(
            AIProviderException ex,
            HttpServletRequest request
    ) {

        Map<String, Object> body = new HashMap<>();

        body.put("timestamp", LocalDateTime.now());
        body.put("status", ex.getStatus());
        body.put("error", ex.getMessage());
        body.put("path", request.getRequestURI());

        return ResponseEntity
                .status(ex.getStatus())
                .body(body);
    }

    @ExceptionHandler(OverpassUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleOverpassUnavailable(
            OverpassUnavailableException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", ex.getMessage()));
    }
}
