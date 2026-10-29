package com.example.espelhovitalback.service;

import com.example.espelhovitalback.config.CurrentUser;
import com.example.espelhovitalback.model.CicloMenstrual;
import com.example.espelhovitalback.repository.CicloMenstrualRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class CycleHistoryService {
    public record Interval(LocalDate inicio, LocalDate proximoInicio, int dias) {
    }

    public record History(List<Interval> intervalos, Double mediaDias, Integer menorIntervalo, Integer maiorIntervalo,
                          Integer variacaoDias, String observacao) {
    }

    private final CicloMenstrualRepository cycles;
    private final CurrentUser current;

    public CycleHistoryService(CicloMenstrualRepository cycles, CurrentUser current) {
        this.cycles = cycles;
        this.current = current;
    }

    public History history() {
        return calculate(cycles.findByUsuarioIdOrderByDataUltimaMenstruacaoAscIdAsc(current.id()));
    }

    public History calculate(List<CicloMenstrual> records) {
        List<LocalDate> dates = records.stream().map(CicloMenstrual::getDataUltimaMenstruacao).filter(Objects::nonNull).distinct().sorted().toList();
        List<Interval> intervals = new ArrayList<>();
        for (int i = 1; i < dates.size(); i++)
            intervals.add(new Interval(dates.get(i - 1), dates.get(i), (int) ChronoUnit.DAYS.between(dates.get(i - 1), dates.get(i))));
        var stats = intervals.stream().mapToInt(Interval::dias).summaryStatistics();
        return new History(intervals, intervals.isEmpty() ? null : Math.round(stats.getAverage() * 10) / 10.0,
                intervals.isEmpty() ? null : stats.getMin(), intervals.isEmpty() ? null : stats.getMax(), intervals.isEmpty() ? null : stats.getMax() - stats.getMin(),
                "Intervalos entre inícios registrados, sem incluir previsões. Lacunas no histórico podem aumentar os valores. A variação não é um diagnóstico.");
    }
}
