package com.example.espelhovitalback.dto;

import com.example.espelhovitalback.model.Diario.*;
import jakarta.validation.constraints.*;

import java.util.List;

public record DiarioRequest(
        @NotNull Fluxo fluxo,
        @Min(0) @Max(10) Integer intensidadeDor,
        @NotNull @Size(max = 12) List<@NotNull Sintoma> sintomas,
        @Size(max = 255) String humor,
        @DecimalMin("0.0") @DecimalMax("24.0") Double horasSono,
        @Pattern(regexp = "|Excelente|Boa|Regular|Ruim") String qualidadeSono,
        @Size(max = 2000) String observacoes) {
}
