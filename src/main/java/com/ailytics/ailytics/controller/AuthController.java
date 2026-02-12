package com.ailytics.ailytics.controller;

import com.ailytics.ailytics.security.JwtUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationProvider authenticationProvider;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_STAFF")
                .replace("ROLE_", "");

        String token = jwtUtils.generateToken(request.getUsername(), role);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", role,
                "username", request.getUsername()
        ));
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
