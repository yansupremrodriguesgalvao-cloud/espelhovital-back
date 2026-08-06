package com.example.espelhovitalback.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "ciclo_menstrual")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CicloMenstrual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dataUltimaMenstruacao;

    private Integer duracaoCiclo;

    private Integer duracaoMenstruacao;

    private LocalDate proximaMenstruacao;

    private LocalDate ovulacao;

    private LocalDate inicioPeriodoFertil;

    private LocalDate fimPeriodoFertil;
}