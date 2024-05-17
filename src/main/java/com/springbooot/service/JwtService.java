package com.springbooot.service;

import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;

import com.springbooot.entities.User;

public interface JwtService {
    @Transactional(readOnly = true)
    String extractUserName(String token);

    @Transactional
    String generateToken(UserDetails userDetails);

    @Transactional(readOnly = true)
    boolean isTokenValid(String token, UserDetails userDetails);

    @Transactional(readOnly = true)
    boolean isTokenExpired(String token);

    @Transactional
    String generateRefeshToken(Map<String, Object> extraClaims, UserDetails userDetails);

    @Transactional(readOnly = true)
    boolean isTokenLoggedOut(String username, String token);

    @Transactional(readOnly = true)
    Optional<User> findByVerificationToken(String verificationToken);

    @Transactional(readOnly = true)
    String extractTokenFromHeader(String header);
}
