package com.example.espelhovitalback.dto;

import com.example.espelhovitalback.model.Diario.*;

import java.time.*;
import java.util.List;

public record DiarioResponse(LocalDate data, boolean registrado, Fluxo fluxo, Integer intensidadeDor,
                             List<Sintoma> sintomas, String humor, Double horasSono, String qualidadeSono,
                             String observacoes, Instant atualizadoEm) {
}
