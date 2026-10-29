package com.example.espelhovitalback.dto;

import jakarta.validation.constraints.*;
import com.example.espelhovitalback.model.Sono;

public record SonoRequest(@NotNull @DecimalMin("0.0") @DecimalMax("24.0") Double horas,
                          @Pattern(regexp = "|Excelente|Boa|Regular|Ruim") String qualidade) {
    public Sono toEntity() {
        Sono value = new Sono();
        value.setHoras(horas);
        value.setQualidade(qualidade);
        return value;
    }
}
