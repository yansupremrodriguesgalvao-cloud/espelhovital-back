package com.example.espelhovitalback.dto;

import jakarta.validation.constraints.*;
import com.example.espelhovitalback.model.Humor;

public record HumorRequest(@NotBlank @Size(max = 255) String estado, @Size(max = 500) String observacao) {
    public Humor toEntity() {
        Humor value = new Humor();
        value.setEstado(estado);
        value.setObservacao(observacao);
        return value;
    }
}
