package com.springbooot.service.impl;

import java.util.HashMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.springbooot.dto.response.ErrorResponse;
import com.springbooot.dto.response.ForgotPasswordResponse;
import com.springbooot.dto.response.JwtAuthenticationResponse;
import com.springbooot.dto.response.ResetPasswordResponse;
import com.springbooot.dto.response.UserResponse;
import com.springbooot.dto.response.VerificationResponse;
import com.springbooot.entities.Role;
import com.springbooot.entities.User;
import com.springbooot.repository.UserRepository;
import com.springbooot.service.AuthenticationService;
import com.springbooot.service.JwtService;
import com.springbooot.util.EmailUtil;

import jakarta.mail.MessagingException;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailUtil emailUtil;

    public AuthenticationServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtService jwtService, EmailUtil emailUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.emailUtil = emailUtil;
    }

    @Override
    @Transactional
    public UserResponse signup(SignUpRequest signUpRequest) {
        
        UserResponse userResponse = new UserResponse();
        /**
         * Check if the email is already registered
         */
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            String errorMessage = "Email has already been registered!";
            userResponse.setMessage(errorMessage);
            return userResponse;
        }
        /**
         * Validate email and password format
         */
        if (!isValidEmail(signUpRequest.getEmail()) || !isValidPassword(signUpRequest.getPassword())) {
            String errorMessage = "Invalid email or password format!";
            userResponse.setMessage(errorMessage);
            return userResponse;
        }
        /**
         * Create a new user and set information from SignUpRequest
         */
        User user = new User();
        user.setEmail(signUpRequest.getEmail());
        user.setFirstname(signUpRequest.getFirstname());
        user.setLastname(signUpRequest.getLastname());
        user.setRole(Role.USER);
        /**
         * Encrypt password and generate verification token
         */
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        String verificationToken = jwtService.generateToken(user);
        user.setVerificationToken(verificationToken);

        User savedUser = userRepository.save(user);
        /**
         * Send verification email
         */
        emailUtil.sendVerificationEmail(user.getEmail(), user);
        userResponse.setMessage("Signup successful");

        /**
         *  Map the saved user to UserResponse and return
         */
        return mapToUserResponse(savedUser);
    }

    @Override
    @Transactional
    public JwtAuthenticationResponse signin(SignInRequest signInRequest) {
        
        UserResponse userResponse = new UserResponse();
        
        if (!isValidEmail(signInRequest.getEmail()) || !isValidPassword(signInRequest.getPassword())) {
            String errorMessage = ("Invalid email or password format!");
            userResponse.setMessage(errorMessage);
        }
        var user = userRepository.findByEmail(signInRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password!"));
        
        if (!user.isVerified()) {
            String errorMessage = ("Email has not been verified!");
            userResponse.setMessage(errorMessage);
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(signInRequest.getEmail(), signInRequest.getPassword()));
        } catch (AuthenticationException e) {
            throw new IllegalArgumentException("Invalid email or password!");
        }
        var jwt = jwtService.generateToken(user);
        var refeshToken = jwtService.generateRefeshToken(new HashMap<>(), user);

        JwtAuthenticationResponse jwtAuthenticationResponse = new JwtAuthenticationResponse();

        jwtAuthenticationResponse.setToken(jwt);
        jwtAuthenticationResponse.setRefeshToken(refeshToken);
        return jwtAuthenticationResponse;
    }

    @Override
    @Transactional
    public VerificationResponse verifyEmail(String verificationToken) {
        User user = userRepository.findByVerificationToken(verificationToken)
                .orElseThrow(() -> new RuntimeException("Invalid verification token!"));
        
        VerificationResponse response = new VerificationResponse();
        
        /**
         * Check if the token code is valid and hasn't been verified.
         */
        if (jwtService.isTokenValid(verificationToken, user) && !user.isVerified()) {
            user.setVerified(true);
            user.setVerificationToken(null);
            userRepository.save(user);
    
            response.setMessage("Email verification successful");
            response.setSuccess(true);
        } else {
            response.setMessage("Invalid verification token!");
            response.setSuccess(false);
        }
        
        return response;
    }

    @Override
    @Transactional
    public UserResponse logout(SignOutRequest signOutRequest, String token) {
        String userEmail = signOutRequest.getEmail();
        ErrorResponse errorResponse = new ErrorResponse();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found" + userEmail));

        /**
         * Check if the token has been logged out before adding it to the list.
         */
        if (!jwtService.isTokenLoggedOut(userEmail, token)) {
            user.getLoggedOutTokens().add(token);
            user.getLoggedOutTokens().removeIf(t -> t.equals(jwtService.extractTokenFromHeader(token)));
            userRepository.save(user);
            return mapToUserResponse(user);
        } else {
      
            errorResponse.setMessage("Token has already been logged out.");
        }
        return null;
    }

    @Override
    @Transactional
    public ForgotPasswordResponse forgotPasswordRequest(ForgotPasswordRequest forgotPasswordRequest) {
        String email = forgotPasswordRequest.getEmail();
        String message;

        try {
            emailUtil.sendResetPasswordEmail(email);
            message = "Reset password email has been sent successfully.";
        } catch (MessagingException e) {
            message = "Unable to send email, please try again later.";
            throw new RuntimeException(message);
        }

        /**
         * Create ForgotPasswordResponse with message and email
         */
        ForgotPasswordResponse forgotPasswordResponse = new ForgotPasswordResponse();
        forgotPasswordResponse.setMessage(message);
        forgotPasswordResponse.setEmail(email);

        return forgotPasswordResponse;
    }

    @Override
    @Transactional
    public ResetPasswordResponse resetPasswordRequest(ResetPasswordRequest resetPasswordRequest) {
        String email = resetPasswordRequest.getEmail();
        String newPassword = resetPasswordRequest.getNewPassword();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        emailUtil.sendChangePasswordEmail(user.getEmail());

        return new ResetPasswordResponse("Mật khẩu đã được đặt lại thành công cho người dùng: " + email);
    }

    @Override
    @Transactional
    public JwtAuthenticationResponse refeshToken(RefeshTokenRequest refeshTokenRequest) {
        String userEmail = jwtService.extractUserName(refeshTokenRequest.getToken());
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        if (jwtService.isTokenValid(refeshTokenRequest.getToken(), user)) {
            var jwt = jwtService.generateToken(user);

            JwtAuthenticationResponse jwtAuthenticationResponse = new JwtAuthenticationResponse();

            jwtAuthenticationResponse.setToken(jwt);
            jwtAuthenticationResponse.setRefeshToken(refeshTokenRequest.getToken());
            return jwtAuthenticationResponse;
        }
        return null;
    }

    @Override
    @Transactional
    public ChangePasswordResponse changePassword(ChangePasswordRequest changePasswordRequest) {
        String email = changePasswordRequest.getEmail();
        String oldPassword = changePasswordRequest.getOldPassword();
        String newPassword = changePasswordRequest.getNewPassword();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect old password.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        emailUtil.sendChangePasswordEmail(user.getEmail());

        return new ChangePasswordResponse("Password has been successfully changed for user: " + email);
    }

    @Override
    @Transactional
    public ChangeEmailResponse changeEmail(ChangeEmailRequest changeEmailRequest) {
        String oldEmail = changeEmailRequest.getOldEmail();
        User user = userRepository.findByEmail(oldEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + oldEmail));

        String otp = emailUtil.generateOtp();
        user.setOtp(otp);
        userRepository.save(user);

        emailUtil.sendChangeEmail(oldEmail, otp);

        return new ChangeEmailResponse("An OTP has been sent to your old email address to confirm the email change.");
    }

    @Override
    @Transactional
    public VerificationResponse verifyOtp(String email, String newEmail, String otp) {

        VerificationResponse response = new VerificationResponse();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        if (user.getOtp() != null && otp.equals(user.getOtp())) {
            user.setEmail(newEmail);
            user.setOtp(null);
            userRepository.save(user);

            response.setMessage("Email verification successful");
            response.setSuccess(true);
            return response;
        } else {
            response.setMessage("Invalid or expired OTP");
            response.setSuccess(false);
            return response;
        }
    }

    @Override
    @Transactional
    public Page<UserResponse> searchUsers(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Specification<User> spec = (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("firstname")), "%" + keyword.toLowerCase() + "%"),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("lastname")), "%" + keyword.toLowerCase() + "%"),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), "%" + keyword.toLowerCase() + "%"));

        Page<User> userPage = userRepository.findAll(spec, pageable);
        return userPage.map(this::mapToUserResponse);
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse userResponse = new UserResponse();
        userResponse.setEmail(user.getEmail());
        userResponse.setFirstname(user.getFirstname());
        userResponse.setLastname(user.getLastname());
        userResponse.setRole(user.getRole());
        /**
         * Map other user-related fields as needed
         */
        return userResponse;
    }
    
    public boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }

    public boolean isValidPassword(String password) {
        return password != null && password.length() >= 4;
    }


}
