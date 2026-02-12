package com.ailytics.ailytics.repository;

import com.ailytics.ailytics.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    List<AppUser> findByRole(AppUser.Role role);
}
