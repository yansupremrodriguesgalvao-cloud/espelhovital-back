package com.example.espelhovitalback.repository;

import com.example.espelhovitalback.model.Diario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.util.*;

public interface DiarioRepository extends JpaRepository<Diario, Long> {
    long countByUsuarioId(Long usuarioId);

    Optional<Diario> findByUsuarioIdAndDataRegistro(Long usuarioId, LocalDate data);

    Page<Diario> findByUsuarioIdAndDataRegistroBetween(Long usuarioId, LocalDate inicio, LocalDate fim, Pageable pageable);

    List<Diario> findByUsuarioIdOrderByDataRegistroAsc(Long usuarioId);

    List<Diario> findByUsuarioIdAndDataRegistroBetweenOrderByDataRegistroAsc(Long usuarioId, LocalDate inicio, LocalDate fim);

    void deleteByUsuarioId(Long usuarioId);
}
