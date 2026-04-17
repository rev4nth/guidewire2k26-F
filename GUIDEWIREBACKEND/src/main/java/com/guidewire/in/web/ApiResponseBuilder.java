package com.guidewire.in.web;

import com.guidewire.in.dto.ApiFailedResponse;
import com.guidewire.in.dto.ApiSuccessResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ApiResponseBuilder {

	private ApiResponseBuilder() {
	}

	public static <T> ResponseEntity<ApiSuccessResponse<T>> ok(String message, T data) {
		return ResponseEntity.ok(new ApiSuccessResponse<>(message, data));
	}

	public static <T> ResponseEntity<ApiSuccessResponse<T>> created(String message, T data) {
		return ResponseEntity.status(HttpStatus.CREATED).body(new ApiSuccessResponse<>(message, data));
	}

	public static ResponseEntity<ApiFailedResponse> fail(HttpStatus httpStatus, String error) {
		return ResponseEntity.status(httpStatus).body(new ApiFailedResponse(error));
	}
}
