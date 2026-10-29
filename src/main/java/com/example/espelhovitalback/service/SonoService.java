package com.example.espelhovitalback.service;


import com.example.espelhovitalback.model.Sono;
import com.example.espelhovitalback.repository.SonoRepository;
import com.example.espelhovitalback.config.CurrentUser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@org.springframework.transaction.annotation.Transactional
public class SonoService {
    private final SonoRepository repository;
    private final CurrentUser current;
    private final DailySignals signals;
    private final com.example.espelhovitalback.repository.UsuarioRepository users;

    public SonoService(SonoRepository repository, CurrentUser current, DailySignals signals, com.example.espelhovitalback.repository.UsuarioRepository users) {
        this.signals = signals;
        this.users = users;
        this.repository = repository;
        this.current = current;
    }

    public com.example.espelhovitalback.dto.HistoryPage<Sono> historico(int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100)
            throw new IllegalArgumentException("Página ou tamanho inválido.");
        var page = org.springframework.data.domain.PageRequest.of(pagina, tamanho, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "dataRegistro", "id"));
        return com.example.espelhovitalback.dto.HistoryPage.of(repository.findByUsuarioId(current.id(), page));
    }

    public List<Sono> listar() {
        return repository.findByUsuarioIdOrderByDataRegistroAscIdAsc(current.id());
    }

    public Sono salvar(Sono body) {
        Long owner = current.id();
        users.lockById(owner).orElseThrow();
        return signals.sleep(owner, java.time.LocalDate.now(), body.getHoras(), body.getQualidade());
    }
}
