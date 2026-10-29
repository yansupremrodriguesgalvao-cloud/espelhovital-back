package com.example.espelhovitalback.dto;

import com.example.espelhovitalback.model.*;

import java.time.*;
import java.util.List;

public record ReportData(String nome, LocalDate inicio, LocalDate fim, Instant geradoEm,
                         List<DiarioResponse> diario, List<Sono> sono, List<Humor> humor, List<CicloMenstrual> ciclos,
                         List<Consulta> consultas,
                         List<String> secoes) {
}
