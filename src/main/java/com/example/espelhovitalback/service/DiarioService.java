package com.example.espelhovitalback.service;

import com.example.espelhovitalback.config.CurrentUser;
import com.example.espelhovitalback.dto.*;
import com.example.espelhovitalback.model.*;
import com.example.espelhovitalback.model.Diario.*;
import com.example.espelhovitalback.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DiarioService {
    private final DiarioRepository diary;
    private final SonoRepository sleeps;
    private final HumorRepository moods;
    private final UsuarioRepository users;
    private final CurrentUser current;
    private final DailySignals signals;
    private final Clock clock;

    public DiarioService(DiarioRepository diary, SonoRepository sleeps, HumorRepository moods, UsuarioRepository users,
                         CurrentUser current, DailySignals signals, Clock clock) {
        this.diary = diary;
        this.sleeps = sleeps;
        this.moods = moods;
        this.users = users;
        this.current = current;
        this.signals = signals;
        this.clock = clock;
    }

    public DiarioResponse get(LocalDate date) {
        validateDate(date);
        Long owner = current.id();
        return view(owner, date, diary.findByUsuarioIdAndDataRegistro(owner, date).orElse(null));
    }

    public HistoryPage<DiarioResponse> history(LocalDate start, LocalDate end, int page, int size) {
        validateRange(start, end);
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Página ou tamanho inválido.");
        Long owner = current.id();
        var result = diary.findByUsuarioIdAndDataRegistroBetween(owner, start, end,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dataRegistro")));
        Map<LocalDate, Sono> sleepByDate = new HashMap<>();
        sleeps.findByUsuarioIdAndDataRegistroBetweenOrderByDataRegistroAscIdAsc(owner, start, end).forEach(r -> sleepByDate.put(r.getDataRegistro(), r));
        Map<LocalDate, Humor> moodByDate = new HashMap<>();
        moods.findByUsuarioIdAndDataRegistroBetweenOrderByDataRegistroAscIdAsc(owner, start, end).forEach(r -> moodByDate.put(r.getDataRegistro(), r));
        return HistoryPage.of(result.map(r -> response(r.getDataRegistro(), r, sleepByDate.get(r.getDataRegistro()), moodByDate.get(r.getDataRegistro()))));
    }

    public DiarioResponse save(LocalDate date, DiarioRequest body) {
        validateDate(date);
        Long owner = current.id();
        users.lockById(owner).orElseThrow();
        if (body.fluxo() == Fluxo.NAO_INFORMADO && body.intensidadeDor() == null && body.sintomas().isEmpty()
                && blank(body.humor()) && body.horasSono() == null && blank(body.observacoes()))
            throw new IllegalArgumentException("Preencha pelo menos uma informação do seu dia.");
        if (body.horasSono() == null && !blank(body.qualidadeSono()))
            throw new IllegalArgumentException("Informe as horas de sono para registrar a qualidade.");
        Diario entry = diary.findByUsuarioIdAndDataRegistro(owner, date).orElseGet(Diario::new);
        entry.setUsuarioId(owner);
        entry.setDataRegistro(date);
        entry.setFluxo(body.fluxo());
        entry.setIntensidadeDor(body.intensidadeDor());
        entry.setSintomas(body.sintomas().stream().distinct().map(Enum::name).collect(Collectors.joining(",")));
        entry.setObservacoes(blank(body.observacoes()) ? null : body.observacoes().trim());
        entry.setAtualizadoEm(clock.instant());
        if (body.horasSono() != null)
            entry.setSonoId(signals.sleep(owner, date, body.horasSono(), body.qualidadeSono()).getId());
        else {
            if (entry.getSonoId() != null)
                sleeps.findByIdAndUsuarioId(entry.getSonoId(), owner).ifPresent(sleeps::delete);
            entry.setSonoId(null);
        }
        if (!blank(body.humor())) entry.setHumorId(signals.mood(owner, date, body.humor(), null).getId());
        else {
            if (entry.getHumorId() != null)
                moods.findByIdAndUsuarioId(entry.getHumorId(), owner).ifPresent(moods::delete);
            entry.setHumorId(null);
        }
        diary.saveAndFlush(entry);
        return view(owner, date, entry);
    }

    public void delete(LocalDate date) {
        validateDate(date);
        Long owner = current.id();
        users.lockById(owner).orElseThrow();
        Diario entry = diary.findByUsuarioIdAndDataRegistro(owner, date)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não há registro do diário nesta data."));
        if (entry.getSonoId() != null) sleeps.findByIdAndUsuarioId(entry.getSonoId(), owner).ifPresent(sleeps::delete);
        if (entry.getHumorId() != null) moods.findByIdAndUsuarioId(entry.getHumorId(), owner).ifPresent(moods::delete);
        diary.delete(entry);
    }

    private DiarioResponse view(Long owner, LocalDate date, Diario entry) {
        Sono sleep = sleeps.findTopByUsuarioIdAndDataRegistroOrderByIdDesc(owner, date).orElse(null);
        Humor mood = moods.findTopByUsuarioIdAndDataRegistroOrderByIdDesc(owner, date).orElse(null);
        return response(date, entry, sleep, mood);
    }

    private DiarioResponse response(LocalDate date, Diario entry, Sono sleep, Humor mood) {
        return new DiarioResponse(date, entry != null, entry == null ? Fluxo.NAO_INFORMADO : entry.getFluxo(), entry == null ? null : entry.getIntensidadeDor(),
                entry == null || entry.getSintomas().isBlank() ? List.of() : Arrays.stream(entry.getSintomas().split(",")).map(Sintoma::valueOf).toList(),
                mood == null ? null : mood.getEstado(), sleep == null ? null : sleep.getHoras(), sleep == null ? null : sleep.getQualidade(),
                entry == null ? null : entry.getObservacoes(), entry == null ? null : entry.getAtualizadoEm());
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void validateDate(LocalDate date) {
        if (date == null || date.isAfter(LocalDate.now(clock)) || date.isBefore(LocalDate.of(1900, 1, 1)))
            throw new IllegalArgumentException("Escolha uma data válida, até hoje.");
    }

    public void validateRange(LocalDate start, LocalDate end) {
        validateDate(start);
        validateDate(end);
        if (end.isBefore(start) || java.time.temporal.ChronoUnit.DAYS.between(start, end) > 365)
            throw new IllegalArgumentException("Escolha um período de até 366 dias, com início anterior ao fim.");
    }
}
