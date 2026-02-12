package com.ailytics.ailytics.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String username;
    private String password;
    
    @Enumerated(EnumType.STRING)
    private Role role;

    public enum Role {
        MASTER, STAFF
    }
}
