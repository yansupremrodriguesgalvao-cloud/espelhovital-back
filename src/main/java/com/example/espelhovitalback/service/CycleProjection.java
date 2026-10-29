package com.example.espelhovitalback.service;


import com.example.espelhovitalback.dto.CalendarioResponse;
import com.example.espelhovitalback.dto.CalendarioResponse.*;
import com.example.espelhovitalback.model.CicloMenstrual;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Projections never create confirmed menstrual records. Date arithmetic uses LocalDate only.
 */
@Component
public class CycleProjection {
    public CalendarioResponse project(List<CicloMenstrual> history, LocalDate today) {
        List<CicloMenstrual> sorted = history.stream().filter(c -> c.getDataUltimaMenstruacao() != null)
                .sorted(Comparator.comparing(CicloMenstrual::getDataUltimaMenstruacao)).toList();
        LocalDate limit = today.plusMonths(24);
        if (sorted.isEmpty()) return new CalendarioResponse(List.of(), List.of(), false, 28, 28, 28, 0, false, limit,
                "Registre o início de uma menstruação para acompanhar o calendário.");
        List<Registro> records = sorted.stream().map(c -> new Registro(c.getId(), c.getDataUltimaMenstruacao(),
                c.getDuracaoMenstruacao() == null ? 5 : c.getDuracaoMenstruacao(), c.getDuracaoCiclo() == null ? 28 : c.getDuracaoCiclo(),
                Boolean.TRUE.equals(c.getIrregular()), c.getDuracaoMinima(), c.getDuracaoMaxima(), c.getFimMenstruacao())).toList();
        CicloMenstrual last = sorted.get(sorted.size() - 1);
        List<Integer> intervals = new ArrayList<>();
        for (int i = Math.max(1, sorted.size() - 6); i < sorted.size(); i++) {
            int days = (int) ChronoUnit.DAYS.between(sorted.get(i - 1).getDataUltimaMenstruacao(), sorted.get(i).getDataUltimaMenstruacao());
            // Very short/long gaps stay in history but do not silently distort the estimate.
            if (days >= 15 && days <= 180) intervals.add(days);
        }
        List<Integer> ordered = intervals.stream().sorted().toList();
        int n = ordered.size();
        int typical = n == 0 ? (last.getDuracaoCiclo() == null ? 28 : last.getDuracaoCiclo())
                : n % 2 == 1 ? ordered.get(n / 2) : (int) Math.round((ordered.get(n / 2 - 1) + ordered.get(n / 2)) / 2.0);
        int min = n == 0 ? typical : ordered.get(0);
        int max = n == 0 ? typical : ordered.get(n - 1);
        // Variability is an interface estimate, not a clinical diagnosis.
        boolean irregular = Boolean.TRUE.equals(last.getIrregular()) || (n >= 2 && max - min > 7);
        if (irregular) {
            min = last.getDuracaoMinima() == null ? Math.max(15, min - 3) : Math.min(min, last.getDuracaoMinima());
            max = last.getDuracaoMaxima() == null ? Math.min(180, max + 3) : Math.max(max, last.getDuracaoMaxima());
        }
        typical = Math.max(min, Math.min(max, typical));
        LocalDate start = last.getDataUltimaMenstruacao();
        List<Previsao> forecasts = new ArrayList<>();
        // An index cap also bounds work for unusually old histories.
        int firstIndex = Math.max(1, (int) (ChronoUnit.DAYS.between(start, today.minusMonths(12)) / max));
        for (int k = firstIndex; k <= firstIndex + 10000; k++) {
            LocalDate predicted = start.plusDays((long) k * typical);
            LocalDate early = start.plusDays((long) k * min);
            LocalDate late = start.plusDays((long) k * max);
            if (early.isAfter(limit)) break;
            if (late.isBefore(today.minusMonths(12))) continue;
            LocalDate ovulation = irregular ? null : predicted.minusDays(14);
            forecasts.add(new Previsao(predicted, predicted.plusDays((last.getDuracaoMenstruacao() == null ? 5 : last.getDuracaoMenstruacao()) - 1),
                    early, late, ovulation, ovulation == null ? null : ovulation.minusDays(5), ovulation == null ? null : ovulation.plusDays(1)));
        }
        boolean pending = today.isAfter(start.plusDays(max));
        return new CalendarioResponse(records, forecasts, irregular, typical, min, max, n, pending, limit,
                irregular ? "Ciclo irregular: os intervalos são estimativas e ficam mais amplos no futuro. Não estimamos ovulação ou período fértil. Registre cada menstruação para atualizar."
                        : "As previsões são estimativas, não confirmam ovulação e não devem ser usadas como método contraceptivo. Registre cada menstruação para atualizar.");
    }
}
