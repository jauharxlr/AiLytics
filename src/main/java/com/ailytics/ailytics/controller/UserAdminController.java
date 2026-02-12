package com.ailytics.ailytics.controller;

import com.ailytics.ailytics.model.AppUser;
import com.ailytics.ailytics.repository.AppUserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for Master user to manage Staff accounts")
@PreAuthorize("hasRole('MASTER')")
public class UserAdminController {

    private final AppUserRepository userRepository;

    @Operation(summary = "List all staff users")
    @GetMapping
    public ResponseEntity<List<AppUser>> listUsers() {
        return ResponseEntity.ok(userRepository.findByRole(AppUser.Role.STAFF));
    }

    @Operation(summary = "Create a new staff user")
    @PostMapping
    public ResponseEntity<AppUser> createStaff(@RequestBody AppUser user) {
        user.setRole(AppUser.Role.STAFF);
        return ResponseEntity.ok(userRepository.save(user));
    }

    @Operation(summary = "Delete a staff user")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
