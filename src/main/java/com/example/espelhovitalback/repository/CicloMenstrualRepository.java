package com.example.espelhovitalback.repository;


import com.example.espelhovitalback.model.CicloMenstrual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface CicloMenstrualRepository extends JpaRepository<CicloMenstrual, Long> {
    long countByUsuarioId(Long usuarioId);

    List<CicloMenstrual> findByUsuarioIdAndDataUltimaMenstruacaoBetweenOrderByDataUltimaMenstruacaoAscIdAsc(Long usuarioId, java.time.LocalDate start, java.time.LocalDate end);

    void deleteByUsuarioId(Long usuarioId);

    List<CicloMenstrual> findByUsuarioIdOrderByDataUltimaMenstruacaoAscIdAsc(Long usuarioId);

    Optional<CicloMenstrual> findByIdAndUsuarioId(Long id, Long usuarioId);
}
