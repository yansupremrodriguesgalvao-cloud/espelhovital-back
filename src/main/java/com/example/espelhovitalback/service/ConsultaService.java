package com.example.espelhovitalback.service;

import com.example.espelhovitalback.model.Consulta;
import com.example.espelhovitalback.repository.ConsultaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConsultaService {

    @Autowired
    private ConsultaRepository repository;

    public List<Consulta> listarTodas() {
        return repository.findAllByOrderByDataAgendadaAscHoraAgendadaAsc();
    }

    public List<Consulta> listarPorUsuario(Long usuarioId) {
        return repository.findByUsuarioIdOrderByDataAgendadaAsc(usuarioId);
    }

    public Consulta salvar(Consulta consulta) {
        return repository.save(consulta);
    }

    public Consulta atualizar(Long id, Consulta dados) {
        Consulta existente = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Compromisso não encontrado"));

        existente.setTipo(dados.getTipo());
        existente.setTitulo(dados.getTitulo());
        existente.setLocal(dados.getLocal());
        existente.setProfissional(dados.getProfissional());
        existente.setDataAgendada(dados.getDataAgendada());
        existente.setHoraAgendada(dados.getHoraAgendada());
        existente.setObservacao(dados.getObservacao());
        if (dados.getStatus() != null) {
            existente.setStatus(dados.getStatus());
        }
        if (dados.getLembreteDiasAntes() != null) {
            existente.setLembreteDiasAntes(dados.getLembreteDiasAntes());
        }

        return repository.save(existente);
    }

    public void excluir(Long id) {
        repository.deleteById(id);
    }

    /** Compromissos cujo lembrete deve disparar hoje (data agendada - dias de antecedência <= hoje). */
    public List<Consulta> lembretesAtivos() {
        LocalDate hoje = LocalDate.now();

        return repository.findAllByOrderByDataAgendadaAscHoraAgendadaAsc().stream()
                .filter(c -> "AGENDADA".equalsIgnoreCase(c.getStatus()))
                .filter(c -> {
                    int diasAntes = c.getLembreteDiasAntes() != null ? c.getLembreteDiasAntes() : 1;
                    LocalDate dataLembrete = c.getDataAgendada().minusDays(diasAntes);
                    long diasAteEvento = ChronoUnit.DAYS.between(hoje, c.getDataAgendada());
                    return !hoje.isAfter(c.getDataAgendada()) && !hoje.isBefore(dataLembrete) || diasAteEvento == 0;
                })
                .collect(Collectors.toList());
    }

    /** Próximos compromissos (ainda não realizados/cancelados), ordenados por data. */
    public List<Consulta> proximos() {
        LocalDate hoje = LocalDate.now();
        return repository.findAllByOrderByDataAgendadaAscHoraAgendadaAsc().stream()
                .filter(c -> "AGENDADA".equalsIgnoreCase(c.getStatus()))
                .filter(c -> !c.getDataAgendada().isBefore(hoje))
                .collect(Collectors.toList());
    }
}
