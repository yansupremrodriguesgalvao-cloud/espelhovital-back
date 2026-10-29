package com.example.espelhovitalback.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.*;

@Entity
@Table(name = "lembrete_entrega")
@Getter
@Setter
public class LembreteEntrega {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long usuarioId;
    @Column(nullable = false)
    private LocalDate dataReferencia;
    @Column(nullable = false, length = 20)
    private String canal;
    @Column(nullable = false, length = 120)
    private String chave;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(nullable = false)
    private int tentativas;
    @Column(nullable = false)
    private Instant ultimaTentativa;
}
