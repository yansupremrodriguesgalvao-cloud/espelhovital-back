package com.example.espelhovitalback.repository;


import com.example.espelhovitalback.model.Humor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface HumorRepository extends JpaRepository<Humor, Long> {
    long countByUsuarioId(Long usuarioId);

    Optional<Humor> findTopByUsuarioIdAndDataRegistroOrderByIdDesc(Long usuarioId, java.time.LocalDate date);

    List<Humor> findByUsuarioIdAndDataRegistroBetweenOrderByDataRegistroAscIdAsc(Long usuarioId, java.time.LocalDate start, java.time.LocalDate end);

    void deleteByUsuarioId(Long usuarioId);

    org.springframework.data.domain.Page<Humor> findByUsuarioId(Long usuarioId, org.springframework.data.domain.Pageable pageable);

    List<Humor> findByUsuarioIdOrderByDataRegistroAscIdAsc(Long usuarioId);

    Optional<Humor> findByIdAndUsuarioId(Long id, Long usuarioId);
}
