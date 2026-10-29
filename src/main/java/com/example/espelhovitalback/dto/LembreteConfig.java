package com.example.espelhovitalback.dto;

import jakarta.validation.constraints.*;

import java.time.LocalTime;
import java.util.List;

public record LembreteConfig(Preferences preferencias, boolean emailDisponivel) {
    public record Preferences(boolean ativo, boolean diario, boolean sono, boolean consultas, boolean ciclo,
                              boolean navegadorAtivo, boolean emailAtivo, @NotNull LocalTime horario,
                              @NotBlank @Size(max = 80) String fusoHorario,
                              @NotNull @Size(min = 1, max = 7) List<@NotNull @Min(1) @Max(7) Integer> diasSemana,
                              @Min(0) @Max(30) int antecedenciaConsulta, @Min(0) @Max(10) int antecedenciaCiclo) {
    }
}
