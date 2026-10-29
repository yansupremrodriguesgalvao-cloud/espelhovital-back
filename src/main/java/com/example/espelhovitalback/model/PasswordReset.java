package com.example.espelhovitalback.model;


import jakarta.persistence.*;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "password_reset")
@Getter
@Setter
public class PasswordReset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private Long usuarioId;
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(nullable = false)
    private Instant expiresAt;
}
