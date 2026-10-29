package com.example.espelhovitalback;

import com.example.espelhovitalback.service.CycleHistoryService;
import com.example.espelhovitalback.model.CicloMenstrual;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CycleHistoryTest {
    private final CycleHistoryService service = new CycleHistoryService(null, null);

    private CicloMenstrual start(String date) {
        var c = new CicloMenstrual();
        c.setDataUltimaMenstruacao(LocalDate.parse(date));
        c.setProximaMenstruacao(LocalDate.parse(date).plusDays(28));
        return c;
    }

    @Test
    void onlyConfirmedDistinctStartsContributeToVariation() {
        var result = service.calculate(List.of(start("2026-03-05"), start("2026-01-01"), start("2026-01-29"), start("2026-01-29")));
        assertThat(result.intervalos()).hasSize(2);
        assertThat(result.menorIntervalo()).isEqualTo(28);
        assertThat(result.maiorIntervalo()).isEqualTo(35);
        assertThat(result.mediaDias()).isEqualTo(31.5);
        assertThat(result.variacaoDias()).isEqualTo(7);
    }

    @Test
    void doesNotInventVariationForOneStart() {
        var result = service.calculate(List.of(start("2026-01-01")));
        assertThat(result.intervalos()).isEmpty();
        assertThat(result.mediaDias()).isNull();
        assertThat(result.variacaoDias()).isNull();
    }
}
