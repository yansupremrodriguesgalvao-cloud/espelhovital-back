package com.example.espelhovitalback;

import com.example.espelhovitalback.model.CicloMenstrual;
import com.example.espelhovitalback.service.CycleProjection;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CycleProjectionTest {
    private final CycleProjection projection = new CycleProjection();

    private CicloMenstrual cycle(String start, int length, boolean irregular) {
        var c = new CicloMenstrual();
        c.setDataUltimaMenstruacao(LocalDate.parse(start));
        c.setDuracaoCiclo(length);
        c.setDuracaoMenstruacao(5);
        c.setIrregular(irregular);
        return c;
    }

    @Test
    void projectsSeveralCyclesAcrossYearBoundaryWithoutCreatingRecords() {
        var record = cycle("2026-12-20", 28, false);
        var result = projection.project(List.of(record), LocalDate.of(2026, 12, 21));
        assertThat(result.registros()).hasSize(1);
        assertThat(result.previsoes()).hasSizeGreaterThan(12);
        assertThat(result.previsoes().get(0).inicio()).isEqualTo(LocalDate.of(2027, 1, 17));
        assertThat(result.previsoes().get(1).inicio()).isEqualTo(LocalDate.of(2027, 2, 14));
        assertThat(result.previsoes().get(0).ovulacao()).isEqualTo(LocalDate.of(2027, 1, 3));
    }

    @Test
    void newConfirmedStartReanchorsAllForecastsAndUsesObservedIntervals() {
        var result = projection.project(List.of(cycle("2026-01-01", 28, false), cycle("2026-02-02", 28, false)), LocalDate.of(2026, 2, 3));
        assertThat(result.duracaoEstimada()).isEqualTo(32);
        assertThat(result.intervalosObservados()).isEqualTo(1);
        assertThat(result.previsoes().get(0).inicio()).isEqualTo(LocalDate.of(2026, 3, 6));
        assertThat(result.registros()).hasSize(2);
    }

    @Test
    void irregularHistoryProducesRangesAndOmitsFertilityClaims() {
        var last = cycle("2026-03-07", 28, true);
        last.setDuracaoMinima(24);
        last.setDuracaoMaxima(45);
        var result = projection.project(List.of(cycle("2026-01-01", 28, false), cycle("2026-01-25", 28, false), last), LocalDate.of(2026, 3, 8));
        assertThat(result.irregular()).isTrue();
        assertThat(result.duracaoMinima()).isEqualTo(24);
        assertThat(result.duracaoMaxima()).isEqualTo(45);
        assertThat(result.previsoes().get(0).inicioMaisCedo()).isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(result.previsoes().get(0).inicioMaisTarde()).isEqualTo(LocalDate.of(2026, 4, 21));
        assertThat(result.previsoes()).allSatisfy(p -> {
            assertThat(p.ovulacao()).isNull();
            assertThat(p.inicioPeriodoFertil()).isNull();
            assertThat(p.fimPeriodoFertil()).isNull();
        });
    }

    @Test
    void accountsForLeapDayAndMissingConfirmation() {
        var result = projection.project(List.of(cycle("2028-02-01", 28, false)), LocalDate.of(2028, 3, 5));
        assertThat(result.previsoes().get(0).inicio()).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThat(result.atualizacaoPendente()).isTrue();
        assertThat(result.registros()).hasSize(1);
    }

    @Test
    void emptyHistoryDoesNotInventPeriods() {
        var result = projection.project(List.of(), LocalDate.of(2026, 10, 28));
        assertThat(result.registros()).isEmpty();
        assertThat(result.previsoes()).isEmpty();
    }
}
