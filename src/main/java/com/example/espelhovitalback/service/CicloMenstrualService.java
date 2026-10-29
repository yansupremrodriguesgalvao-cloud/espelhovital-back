package com.example.espelhovitalback.service;


import com.example.espelhovitalback.dto.*;
import com.example.espelhovitalback.model.CicloMenstrual;
import com.example.espelhovitalback.repository.CicloMenstrualRepository;
import com.example.espelhovitalback.config.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class CicloMenstrualService {
    private final CicloMenstrualRepository repository;
    private final CurrentUser current;
    private final com.example.espelhovitalback.repository.UsuarioRepository users;
    private final CycleProjection projection;

    public CicloMenstrualService(CicloMenstrualRepository repository, CurrentUser current, CycleProjection projection, com.example.espelhovitalback.repository.UsuarioRepository users) {
        this.users = users;
        this.repository = repository;
        this.current = current;
        this.projection = projection;
    }

    public List<CicloMenstrual> listar() {
        return repository.findByUsuarioIdOrderByDataUltimaMenstruacaoAscIdAsc(current.id());
    }

    public CalendarioResponse calendario() {
        return projection.project(listar(), LocalDate.now());
    }

    public CicloResponse calcularCiclo(CicloRequest request) {
        users.lockById(current.id()).orElseThrow();
        // Saving the same date is an update, so a retry cannot create duplicate starts.
        CicloMenstrual cycle = listar().stream().filter(c -> c.getDataUltimaMenstruacao().equals(request.getDataUltimaMenstruacao()))
                .findFirst().orElseGet(CicloMenstrual::new);
        apply(cycle, request);
        repository.save(cycle);
        return new CicloResponse(cycle.getProximaMenstruacao(), cycle.getOvulacao(), cycle.getInicioPeriodoFertil(), cycle.getFimPeriodoFertil());
    }

    public CicloInicioResponse registrarInicio(LocalDate date) {
        if (date == null || date.isAfter(LocalDate.now()) || date.isBefore(LocalDate.of(1900, 1, 1)))
            throw new IllegalArgumentException("Escolha uma data válida, até hoje.");
        users.lockById(current.id()).orElseThrow();
        List<CicloMenstrual> history = listar().stream().filter(c -> c.getDataUltimaMenstruacao() != null).toList();
        if (history.isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Faça seu primeiro cadastro para salvar as informações do ciclo.");
        // Retries and repeated taps must not alter an already confirmed start.
        for (CicloMenstrual existing : history) {
            if (date.equals(existing.getDataUltimaMenstruacao()))
                return new CicloInicioResponse(false, existing.getId(), calendario());
        }

        CicloMenstrual reference = history.get(0);
        for (CicloMenstrual record : history) {
            if (!record.getDataUltimaMenstruacao().isAfter(date)) reference = record;
        }
        int bleedingDays = reference.getDuracaoMenstruacao() == null ? 5 : reference.getDuracaoMenstruacao();
        for (CicloMenstrual record : history) {
            LocalDate start = record.getDataUltimaMenstruacao();
            LocalDate end = start.plusDays(record.getDuracaoMenstruacao() == null ? 5 : record.getDuracaoMenstruacao());
            if (date.isBefore(end) && start.isBefore(date.plusDays(bleedingDays)))
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Esta data se sobrepõe a um período registrado. Use Editar para corrigir o início ou a duração desse período.");
        }

        CicloRequest request = new CicloRequest();
        request.setDataUltimaMenstruacao(date);
        request.setDuracaoMenstruacao(bleedingDays);
        request.setDuracaoCiclo(reference.getDuracaoCiclo() == null ? 28 : reference.getDuracaoCiclo());
        request.setIrregular(Boolean.TRUE.equals(reference.getIrregular()));
        request.setDuracaoMinima(reference.getDuracaoMinima());
        request.setDuracaoMaxima(reference.getDuracaoMaxima());
        CicloMenstrual cycle = new CicloMenstrual();
        apply(cycle, request);
        repository.saveAndFlush(cycle);
        return new CicloInicioResponse(true, cycle.getId(), calendario());
    }

    public CalendarioResponse registrarFim(Long id, LocalDate date) {
        users.lockById(current.id()).orElseThrow();
        CicloMenstrual cycle = find(id);
        if (date == null || date.isAfter(LocalDate.now()) || date.isBefore(cycle.getDataUltimaMenstruacao()))
            throw new IllegalArgumentException("O último dia de sangramento deve estar entre o início registrado e hoje.");
        if (listar().stream().anyMatch(c -> c.getDataUltimaMenstruacao().isAfter(cycle.getDataUltimaMenstruacao())
                && !c.getDataUltimaMenstruacao().isAfter(date)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O fim não pode alcançar outra menstruação registrada.");
        long days = java.time.temporal.ChronoUnit.DAYS.between(cycle.getDataUltimaMenstruacao(), date) + 1;
        cycle.setDuracaoMenstruacao(Math.toIntExact(days));
        cycle.setFimMenstruacao(date);
        repository.saveAndFlush(cycle);
        return calendario();
    }

    public CicloMenstrual atualizar(Long id, CicloRequest request) {
        users.lockById(current.id()).orElseThrow();
        CicloMenstrual cycle = find(id);
        if (listar().stream().anyMatch(c -> !c.getId().equals(id) && c.getDataUltimaMenstruacao().equals(request.getDataUltimaMenstruacao())))
            throw new IllegalArgumentException("Já existe um registro nessa data. Edite o registro existente.");
        apply(cycle, request);
        return repository.save(cycle);
    }

    public void excluir(Long id) {
        repository.delete(find(id));
    }

    private CicloMenstrual find(Long id) {
        return repository.findByIdAndUsuarioId(id, current.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não encontrado."));
    }

    private void apply(CicloMenstrual c, CicloRequest r) {
        if (!java.util.Objects.equals(c.getDataUltimaMenstruacao(), r.getDataUltimaMenstruacao())
                || !java.util.Objects.equals(c.getDuracaoMenstruacao(), r.getDuracaoMenstruacao()))
            c.setFimMenstruacao(null);
        if (r.getDuracaoMenstruacao() > r.getDuracaoCiclo())
            throw new IllegalArgumentException("A duração da menstruação não pode superar a duração do ciclo.");
        if (r.isIrregular() && (r.getDuracaoMinima() == null || r.getDuracaoMaxima() == null))
            throw new IllegalArgumentException("Informe o menor e o maior intervalo do ciclo irregular.");
        if (r.isIrregular() && (r.getDuracaoMinima() > r.getDuracaoMaxima() || r.getDuracaoCiclo() < r.getDuracaoMinima() || r.getDuracaoCiclo() > r.getDuracaoMaxima()))
            throw new IllegalArgumentException("A duração estimada deve estar entre o menor e o maior intervalo.");
        c.setUsuarioId(current.id());
        c.setDataUltimaMenstruacao(r.getDataUltimaMenstruacao());
        c.setDuracaoCiclo(r.getDuracaoCiclo());
        c.setDuracaoMenstruacao(r.getDuracaoMenstruacao());
        c.setIrregular(r.isIrregular());
        c.setDuracaoMinima(r.isIrregular() ? r.getDuracaoMinima() : null);
        c.setDuracaoMaxima(r.isIrregular() ? r.getDuracaoMaxima() : null);
        c.setProximaMenstruacao(r.getDataUltimaMenstruacao().plusDays(r.getDuracaoCiclo()));
        c.setOvulacao(r.isIrregular() ? null : c.getProximaMenstruacao().minusDays(14));
        c.setInicioPeriodoFertil(c.getOvulacao() == null ? null : c.getOvulacao().minusDays(5));
        c.setFimPeriodoFertil(c.getOvulacao() == null ? null : c.getOvulacao().plusDays(1));
    }
}
