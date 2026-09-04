package com.agentguard.exception;
import com.agentguard.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant; import java.util.LinkedHashMap; import java.util.Map;
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class) ResponseEntity<ApiError> notFound(ResourceNotFoundException e, HttpServletRequest r) { return error(HttpStatus.NOT_FOUND, e.getMessage(), r, null); }
    @ExceptionHandler(DuplicateResourceException.class) ResponseEntity<ApiError> conflict(DuplicateResourceException e, HttpServletRequest r) { return error(HttpStatus.CONFLICT, e.getMessage(), r, null); }
    @ExceptionHandler(IntentParsingException.class) ResponseEntity<ApiError> unprocessable(IntentParsingException e, HttpServletRequest r) { return error(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), r, null); }
    @ExceptionHandler(IntentProviderException.class) ResponseEntity<ApiError> providerUnavailable(IntentProviderException e, HttpServletRequest r) { return error(HttpStatus.SERVICE_UNAVAILABLE, "Intent service is temporarily unavailable", r, null); }
    @ExceptionHandler(PaymentProcessingException.class) ResponseEntity<ApiError> paymentFailed(PaymentProcessingException e, HttpServletRequest r) { return error(HttpStatus.BAD_GATEWAY, "Payment order creation failed", r, null); }
    @ExceptionHandler(PaymentNotAllowedException.class) ResponseEntity<ApiError> paymentNotAllowed(PaymentNotAllowedException e, HttpServletRequest r) { return error(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), r, null); }
    @ExceptionHandler(InvalidPaymentAmountException.class) ResponseEntity<ApiError> invalidAmount(InvalidPaymentAmountException e, HttpServletRequest r) { return error(HttpStatus.BAD_REQUEST, e.getMessage(), r, null); }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ApiError> validation(MethodArgumentNotValidException e, HttpServletRequest r) { Map<String,String> fields = new LinkedHashMap<>(); for (FieldError f : e.getBindingResult().getFieldErrors()) fields.put(f.getField(), f.getDefaultMessage()); return error(HttpStatus.BAD_REQUEST, "Request validation failed", r, fields); }
    @ExceptionHandler(Exception.class) ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest r) { return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", r, null); }
    private ResponseEntity<ApiError> error(HttpStatus status, String message, HttpServletRequest request, Map<String,String> fields) { return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI(), fields)); }
}
