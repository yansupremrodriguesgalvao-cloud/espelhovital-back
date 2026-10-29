package com.example.espelhovitalback.service;


import com.example.espelhovitalback.model.Humor;
import com.example.espelhovitalback.repository.HumorRepository;
import com.example.espelhovitalback.config.CurrentUser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@org.springframework.transaction.annotation.Transactional
public class HumorService {
    private final HumorRepository repository;
    private final CurrentUser current;
    private final DailySignals signals;
    private final com.example.espelhovitalback.repository.UsuarioRepository users;

    public HumorService(HumorRepository repository, CurrentUser current, DailySignals signals, com.example.espelhovitalback.repository.UsuarioRepository users) {
        this.signals = signals;
        this.users = users;
        this.repository = repository;
        this.current = current;
    }

    public com.example.espelhovitalback.dto.HistoryPage<Humor> historico(int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100)
            throw new IllegalArgumentException("Página ou tamanho inválido.");
        var page = org.springframework.data.domain.PageRequest.of(pagina, tamanho, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "dataRegistro", "id"));
        return com.example.espelhovitalback.dto.HistoryPage.of(repository.findByUsuarioId(current.id(), page));
    }

    public List<Humor> listar() {
        return repository.findByUsuarioIdOrderByDataRegistroAscIdAsc(current.id());
    }

    public Humor salvar(Humor body) {
        Long owner = current.id();
        users.lockById(owner).orElseThrow();
        return signals.mood(owner, java.time.LocalDate.now(), body.getEstado(), body.getObservacao());
    }
}
