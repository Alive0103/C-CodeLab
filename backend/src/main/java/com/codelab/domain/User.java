package com.codelab.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Column(name = "password_salt", nullable = false, length = 32)
    private String passwordSalt;

    @Column(length = 128)
    private String email;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(length = 32)
    private String role; // ROLE_USER / ROLE_ANONYMOUS

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}


