package com.example.espelhovitalback.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "lembrete_preferencia")
@Getter
@Setter
public class LembretePreferencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private Long usuarioId;
    @Column(nullable = false)
    private boolean ativo;
    @Column(nullable = false)
    private boolean diario = true;
    @Column(nullable = false)
    private boolean sono;
    @Column(nullable = false)
    private boolean consultas = true;
    @Column(nullable = false)
    private boolean ciclo;
    @Column(nullable = false)
    private boolean navegadorAtivo;
    @Column(nullable = false)
    private boolean emailAtivo;
    @Column(nullable = false)
    private LocalTime horario = LocalTime.of(20, 0);
    @Column(nullable = false, length = 80)
    private String fusoHorario = "America/Sao_Paulo";
    @Column(nullable = false, length = 30)
    private String diasSemana = "1,2,3,4,5,6,7";
    @Column(nullable = false)
    private int antecedenciaConsulta = 1;
    @Column(nullable = false)
    private int antecedenciaCiclo = 2;
}
