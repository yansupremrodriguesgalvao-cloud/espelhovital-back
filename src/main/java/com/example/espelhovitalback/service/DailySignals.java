package com.example.espelhovitalback.service;

import com.example.espelhovitalback.model.*;
import com.example.espelhovitalback.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import java.time.LocalDate;

/**
 * Shared daily writes. Callers lock the authenticated user's row within the transaction.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class DailySignals {
    private final SonoRepository sleeps;
    private final HumorRepository moods;

    public DailySignals(SonoRepository sleeps, HumorRepository moods) {
        this.sleeps = sleeps;
        this.moods = moods;
    }

    public Sono sleep(Long owner, LocalDate date, Double hours, String quality) {
        Sono record = sleeps.findTopByUsuarioIdAndDataRegistroOrderByIdDesc(owner, date).orElseGet(Sono::new);
        record.setUsuarioId(owner);
        record.setDataRegistro(date);
        record.setHoras(hours);
        record.setQualidade(quality == null || quality.isBlank() ? hours < 6 ? "Ruim" : hours <= 9 ? "Boa" : "Regular" : quality);
        return sleeps.saveAndFlush(record);
    }

    public Humor mood(Long owner, LocalDate date, String state, String observation) {
        Humor record = moods.findTopByUsuarioIdAndDataRegistroOrderByIdDesc(owner, date).orElseGet(Humor::new);
        record.setUsuarioId(owner);
        record.setDataRegistro(date);
        record.setEstado(state.trim());
        if (observation != null) record.setObservacao(observation);
        return moods.saveAndFlush(record);
    }
}
