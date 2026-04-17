package com.guidewire.in.exception;

import com.guidewire.in.dto.ApiFailedResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiFailedResponse> validation(MethodArgumentNotValidException ex) {
		String msg = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> e.getField() + ": " + e.getDefaultMessage())
				.findFirst()
				.orElse("Validation failed");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiFailedResponse(msg));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiFailedResponse> badRequest(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiFailedResponse(ex.getMessage()));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiFailedResponse> conflict(IllegalStateException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiFailedResponse(ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiFailedResponse> any(Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiFailedResponse(ex.getMessage() != null ? ex.getMessage() : "Internal error"));
	}
}
