package com.springbooot.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.springbooot.dto.request.RefeshTokenRequest;
import com.springbooot.dto.request.SignInRequest;
import com.springbooot.dto.request.SignOutRequest;
import com.springbooot.dto.request.SignUpRequest;
import com.springbooot.dto.response.EmailVerifyResponse;
import com.springbooot.dto.response.JwtAuthenticationResponse;
import com.springbooot.dto.response.SignInResponse;
import com.springbooot.dto.response.UserResponse;
import com.springbooot.service.AuthenticationService;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

	private final AuthenticationService authenticationService;

	public AuthenticationController(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	@PostMapping("/signup")
	public ResponseEntity<?> signup(@RequestBody SignUpRequest signUpRequest) {
	    try {
	        UserResponse userResponse = authenticationService.signup(signUpRequest);
	        if (userResponse.getMessage() != null) {
	            return ResponseEntity.badRequest().body(userResponse.getMessage());
	        }
	        return ResponseEntity.ok("Signup successful");
	    } catch (RuntimeException e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Signup failed! " + e.getMessage());
	    }
	}


	@PostMapping("/signin")
	public ResponseEntity<?> signin(@RequestBody SignInRequest signInRequest) {
		try {
			JwtAuthenticationResponse savedUser = authenticationService.signin(signInRequest);
			return ResponseEntity.ok(savedUser);
		} catch (RuntimeException e) {
			String errorMessage = e.getMessage();
			if (errorMessage == null || errorMessage.isEmpty()) {
				errorMessage = "Internal Server Error";
			}
			SignInResponse signInResponse = new SignInResponse(errorMessage);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(signInResponse);
		}
	}

	@PostMapping("/refesh")
	public ResponseEntity<JwtAuthenticationResponse> refesh(@RequestBody RefeshTokenRequest refeshTokenRequest) {
		return ResponseEntity.ok(authenticationService.refeshToken(refeshTokenRequest));
	}

	@PostMapping("/signout")
	public ResponseEntity<?> signout(@RequestBody SignOutRequest signOutRequest, String token) {
		return ResponseEntity.ok(authenticationService.logout(signOutRequest, token));
	}

	@PutMapping("/verify")
	public ResponseEntity<EmailVerifyResponse> verifyEmail(@RequestParam String token) {
		try {
			authenticationService.verifyEmail(token);
			EmailVerifyResponse responseDTO = new EmailVerifyResponse(true, "Verification successful!");
			return ResponseEntity.ok(responseDTO);
		} catch (IllegalArgumentException e) {
			EmailVerifyResponse responseDTO = new EmailVerifyResponse(false, "Verification failed! " + e.getMessage());
			return ResponseEntity.badRequest().body(responseDTO);
		}
	}

}
