package com.example.espelhovitalback.dto;


import jakarta.validation.constraints.*;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FaceDescriptorRequest {
    private Long usuarioId;
    @Email
    private String email;
    @NotBlank
    @Size(max = 72)
    private String senha;
    @NotNull
    @Size(min = 128, max = 128)
    private List<@NotNull Double> descritor;
}
