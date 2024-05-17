package com.springbooot.dto.response;

public class SignupResponse {
    private String errorMessage;
	public SignupResponse() {
	}
	public SignupResponse(String errorMessage) {
		super();
		this.errorMessage = errorMessage;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

    
}
