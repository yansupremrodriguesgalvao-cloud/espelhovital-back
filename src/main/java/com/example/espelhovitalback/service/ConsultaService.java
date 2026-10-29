package com.example.espelhovitalback.service;


import com.example.espelhovitalback.model.Consulta;
import com.example.espelhovitalback.repository.ConsultaRepository;
import com.example.espelhovitalback.config.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ConsultaService {
    private final ConsultaRepository repository;
    private final CurrentUser current;

    public ConsultaService(ConsultaRepository repository, CurrentUser current) {
        this.repository = repository;
        this.current = current;
    }

    public com.example.espelhovitalback.dto.HistoryPage<Consulta> historico(int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100)
            throw new IllegalArgumentException("Página ou tamanho inválido.");
        var page = org.springframework.data.domain.PageRequest.of(pagina, tamanho, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "dataAgendada", "id"));
        return com.example.espelhovitalback.dto.HistoryPage.of(repository.findByUsuarioId(current.id(), page));
    }

    public List<Consulta> listarTodas() {
        return repository.findByUsuarioIdOrderByDataAgendadaAscHoraAgendadaAscIdAsc(current.id());
    }

    public List<Consulta> listarPorUsuario(Long id) {
        current.requireOwner(id);
        return listarTodas();
    }

    public Consulta buscar(Long id) {
        return repository.findByIdAndUsuarioId(id, current.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compromisso não encontrado."));
    }

    public Consulta salvar(Consulta c) {
        c.setId(null);
        c.setUsuarioId(current.id());
        return repository.save(c);
    }

    public Consulta atualizar(Long id, Consulta c) {
        Consulta existing = buscar(id);
        existing.setTipo(c.getTipo());
        existing.setTitulo(c.getTitulo());
        existing.setLocal(c.getLocal());
        existing.setProfissional(c.getProfissional());
        existing.setDataAgendada(c.getDataAgendada());
        existing.setHoraAgendada(c.getHoraAgendada());
        existing.setObservacao(c.getObservacao());
        if (c.getStatus() != null) existing.setStatus(c.getStatus());
        if (c.getLembreteDiasAntes() != null) existing.setLembreteDiasAntes(c.getLembreteDiasAntes());
        return repository.save(existing);
    }

    public Consulta status(Long id, String status) {
        if (status == null || !status.matches("AGENDADA|REALIZADA|CANCELADA"))
            throw new IllegalArgumentException("Status inválido.");
        Consulta c = buscar(id);
        c.setStatus(status);
        return repository.save(c);
    }

    public void excluir(Long id) {
        repository.delete(buscar(id));
    }

    public List<Consulta> proximos() {
        return repository.findByUsuarioIdAndStatusAndDataAgendadaGreaterThanEqualOrderByDataAgendadaAscHoraAgendadaAsc(current.id(), "AGENDADA", LocalDate.now());
    }

    public List<Consulta> lembretesAtivos() {
        LocalDate hoje = LocalDate.now();
        return proximos().stream()
                .filter(c -> !hoje.isBefore(c.getDataAgendada().minusDays(c.getLembreteDiasAntes() == null ? 1 : c.getLembreteDiasAntes()))).toList();
    }
}
