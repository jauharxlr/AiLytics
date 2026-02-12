package com.ailytics.ailytics.service;

import com.ailytics.ailytics.model.AppUser;
import com.ailytics.ailytics.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    @Value("${auth.master.username}")
    private String masterUsername;

    @Value("${auth.master.password}")
    private String masterPassword;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Check if Master User
        if (masterUsername.equals(username)) {
            return new User(masterUsername, masterPassword, 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")));
        }

        // 2. Check if Staff User in DB
        AppUser appUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new User(appUser.getUsername(), appUser.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name())));
    }
}
