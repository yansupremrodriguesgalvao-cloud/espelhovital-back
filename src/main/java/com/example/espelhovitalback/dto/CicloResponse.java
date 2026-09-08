package com.example.espelhovitalback.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class CicloResponse {

    private LocalDate proximaMenstruacao;

    private LocalDate ovulacao;

    private LocalDate inicioPeriodoFertil;

    private LocalDate fimPeriodoFertil;
}