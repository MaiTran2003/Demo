package com.springbooot.service;

import org.springframework.data.domain.Page;

import com.springbooot.dto.request.ChangeEmailRequest;
import com.springbooot.dto.request.ChangePasswordRequest;
import com.springbooot.dto.request.ForgotPasswordRequest;
import com.springbooot.dto.request.RefeshTokenRequest;
import com.springbooot.dto.request.ResetPasswordRequest;
import com.springbooot.dto.request.SignInRequest;
import com.springbooot.dto.request.SignOutRequest;
import com.springbooot.dto.request.SignUpRequest;
import com.springbooot.dto.response.ChangeEmailResponse;
import com.springbooot.dto.response.ChangePasswordResponse;
import com.springbooot.dto.response.ForgotPasswordResponse;
import com.springbooot.dto.response.JwtAuthenticationResponse;
import com.springbooot.dto.response.ResetPasswordResponse;
import com.springbooot.dto.response.UserResponse;
import com.springbooot.dto.response.VerificationResponse;

public interface AuthenticationService {

	UserResponse signup(SignUpRequest signUpRequest);
	

	JwtAuthenticationResponse signin(SignInRequest signInRequest);
	

	JwtAuthenticationResponse refeshToken(RefeshTokenRequest refeshTokenRequest);
	

	UserResponse logout(SignOutRequest signOutRequest, String token);
	

	VerificationResponse verifyEmail(String verificationToken);
	

	ForgotPasswordResponse forgotPasswordRequest(ForgotPasswordRequest forgotPasswordRequest);
	

        ResetPasswordResponse resetPasswordRequest(ResetPasswordRequest resetPasswordRequest);
	

        ChangePasswordResponse changePassword(ChangePasswordRequest changePasswordRequest);
	

	ChangeEmailResponse changeEmail(ChangeEmailRequest changeEmailRequest);
	

	Page<UserResponse> searchUsers(String keyword, int page, int size);
	
	
	

	VerificationResponse verifyOtp(String email,String newEmail, String otp);
}
