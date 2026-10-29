package com.example.espelhovitalback.repository;

import com.example.espelhovitalback.model.LembreteEntrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.*;

public interface LembreteEntregaRepository extends JpaRepository<LembreteEntrega, Long> {
    Optional<LembreteEntrega> findByUsuarioIdAndDataReferenciaAndCanalAndChave(Long user, LocalDate date, String channel, String key);

    List<LembreteEntrega> findByUsuarioIdAndDataReferenciaAndCanal(Long user, LocalDate date, String channel);

    List<LembreteEntrega> findByUsuarioIdOrderByDataReferenciaAscIdAsc(Long usuarioId);

    void deleteByUsuarioId(Long usuarioId);
}
