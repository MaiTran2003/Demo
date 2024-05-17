package com.springbooot.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.springbooot.dto.request.BorrowBookRequest;
import com.springbooot.dto.request.ChangeEmailRequest;
import com.springbooot.dto.request.ChangePasswordRequest;
import com.springbooot.dto.request.ForgotPasswordRequest;
import com.springbooot.dto.request.ResetPasswordRequest;
import com.springbooot.dto.request.ReturnBookRequest;
import com.springbooot.dto.request.SignOutRequest;
import com.springbooot.dto.response.ChangePasswordResponse;
import com.springbooot.dto.response.ForgotPasswordResponse;
import com.springbooot.dto.response.ResetPasswordResponse;
import com.springbooot.dto.response.UserResponse;
import com.springbooot.service.AuthenticationService;
import com.springbooot.service.BorrowingService;
import com.springbooot.service.JwtService;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/user")
public class UserController {
	
	private final JwtService jwtService;
	
	private final AuthenticationService authenticationService;
	
	private final BorrowingService borrowingService;
	
	public UserController(JwtService jwtService, AuthenticationService authenticationService, BorrowingService borrowingService) {
		super();
		this.jwtService = jwtService;
		this.authenticationService = authenticationService;
		this.borrowingService = borrowingService;
	}


	@GetMapping()
	public ResponseEntity<String> sayHello(){
		return ResponseEntity.ok("Hi User");
	}
	
	@PostMapping("/logout")
	public ResponseEntity<?> signout(@RequestBody SignOutRequest signOutRequest, @RequestHeader("Authorization") String header) {
	    String token = jwtService.extractTokenFromHeader(header);
	    if (token == null) {
	        return ResponseEntity.badRequest().body("Token not found in Authorization header.");
	    }
	    try {
	        UserResponse userResponse = authenticationService.logout(signOutRequest, token);
	        return ResponseEntity.ok("User " + userResponse.getEmail() + " has been logged out successfully.");
	    } catch (RuntimeException e) { 
	        /**
	         * Handle exceptions if there is an error during logout
	         */
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Logout failed! " + e.getMessage());
	    }
	}
	
	@PutMapping("/forgot-password")
	public ResponseEntity<ForgotPasswordResponse> forgotPassword(@RequestBody ForgotPasswordRequest forgotPasswordRequest) {
	    ForgotPasswordResponse response = authenticationService.forgotPasswordRequest(forgotPasswordRequest);
	    return ResponseEntity.ok(response);
	}

	@PutMapping("/reset-password")
	public ResponseEntity<ResetPasswordResponse> resetPassword(@RequestBody ResetPasswordRequest resetPasswordRequest) {
	    ResetPasswordResponse response = authenticationService.resetPasswordRequest(resetPasswordRequest);
	    return ResponseEntity.ok(response);
	}

	@PutMapping("/change-password")
	public ResponseEntity<ChangePasswordResponse> changePassword(@RequestBody ChangePasswordRequest changePasswordRequest) {
	    ChangePasswordResponse response = authenticationService.changePassword(changePasswordRequest);
	    return ResponseEntity.ok(response);
	}

	
	@PostMapping("/change-email")
	public ResponseEntity<String> changeEmail(@RequestBody ChangeEmailRequest changeEmailRequest) {
		 authenticationService.changeEmail(changeEmailRequest);
	        return ResponseEntity.ok("Email change request sent successfully. Please verify the new email address with the OTP sent to your new email.");
	 }

	@PutMapping("/verify-otp")
	public ResponseEntity<?> verifyOtp(@RequestParam("email") String email,@RequestParam("newEmail") String newEmail, @RequestParam("otp") String otp) {
	    try {
	        authenticationService.verifyOtp(email,newEmail, otp);
	        return ResponseEntity.ok("Verification successful!");
	    } catch (RuntimeException e) {
	        return ResponseEntity.badRequest().body("Verification failed! " + e.getMessage());
	    }
	}
	
	@PostMapping("/borrow")
    public ResponseEntity<BorrowBookRequest> borrowBook(@RequestBody BorrowBookRequest borrowBookRequest) {
        try {
            borrowingService.borrowBook(borrowBookRequest);
            return ResponseEntity.ok(borrowBookRequest);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/return")
    public ResponseEntity<ReturnBookRequest> returnBook(@RequestBody ReturnBookRequest returnBookRequest) {
        try {
            borrowingService.returnBook(returnBookRequest);
            return ResponseEntity.ok(returnBookRequest);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
