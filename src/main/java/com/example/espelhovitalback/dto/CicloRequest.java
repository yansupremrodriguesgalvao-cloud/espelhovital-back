package com.example.espelhovitalback.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CicloRequest {

    private LocalDate dataUltimaMenstruacao;
    private Integer duracaoCiclo;
    private Integer duracaoMenstruacao;

}