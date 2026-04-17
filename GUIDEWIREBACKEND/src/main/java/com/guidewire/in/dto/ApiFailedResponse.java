package com.guidewire.in.dto;

public class ApiFailedResponse {

	private final String status = "FAILED";
	private final String error;

	public ApiFailedResponse(String error) {
		this.error = error;
	}

	public String getStatus() {
		return status;
	}

	public String getError() {
		return error;
	}
}
