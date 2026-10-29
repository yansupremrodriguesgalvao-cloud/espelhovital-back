package com.example.espelhovitalback.dto;


import jakarta.validation.constraints.*;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CicloRequest {
    @NotNull
    @PastOrPresent
    private LocalDate dataUltimaMenstruacao;
    @NotNull
    @Min(15)
    @Max(180)
    private Integer duracaoCiclo;
    @NotNull
    @Min(1)
    @Max(15)
    private Integer duracaoMenstruacao;
    private Long usuarioId;
    private boolean irregular;
    @Min(15)
    @Max(180)
    private Integer duracaoMinima;
    @Min(15)
    @Max(180)
    private Integer duracaoMaxima;
}
