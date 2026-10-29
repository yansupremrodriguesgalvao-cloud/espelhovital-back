package com.example.espelhovitalback.repository;


import com.example.espelhovitalback.model.Sono;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface SonoRepository extends JpaRepository<Sono, Long> {
    long countByUsuarioId(Long usuarioId);

    Optional<Sono> findTopByUsuarioIdAndDataRegistroOrderByIdDesc(Long usuarioId, java.time.LocalDate date);

    List<Sono> findByUsuarioIdAndDataRegistroBetweenOrderByDataRegistroAscIdAsc(Long usuarioId, java.time.LocalDate start, java.time.LocalDate end);

    void deleteByUsuarioId(Long usuarioId);

    org.springframework.data.domain.Page<Sono> findByUsuarioId(Long usuarioId, org.springframework.data.domain.Pageable pageable);

    List<Sono> findByUsuarioIdOrderByDataRegistroAscIdAsc(Long usuarioId);

    Optional<Sono> findByIdAndUsuarioId(Long id, Long usuarioId);
}
