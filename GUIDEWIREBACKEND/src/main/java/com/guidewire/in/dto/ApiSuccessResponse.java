package com.guidewire.in.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiSuccessResponse<T> {

	private final String status = "SUCCESS";
	private final String message;
	private final T data;

	public ApiSuccessResponse(String message, T data) {
		this.message = message;
		this.data = data;
	}

	public String getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	public T getData() {
		return data;
	}
}
