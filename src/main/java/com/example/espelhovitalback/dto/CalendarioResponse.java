package com.example.espelhovitalback.dto;


import java.time.LocalDate;
import java.util.List;

public record CalendarioResponse(List<Registro> registros, List<Previsao> previsoes,
                                 boolean irregular, int duracaoEstimada, int duracaoMinima, int duracaoMaxima,
                                 int intervalosObservados, boolean atualizacaoPendente, LocalDate limitePrevisao,
                                 String aviso) {
    public record Registro(Long id, LocalDate inicio, int duracaoMenstruacao, int duracaoCiclo,
                           boolean irregular, Integer duracaoMinima, Integer duracaoMaxima, LocalDate fimMenstruacao) {
    }

    public record Previsao(LocalDate inicio, LocalDate fim, LocalDate inicioMaisCedo,
                           LocalDate inicioMaisTarde, LocalDate ovulacao, LocalDate inicioPeriodoFertil,
                           LocalDate fimPeriodoFertil) {
    }
}
