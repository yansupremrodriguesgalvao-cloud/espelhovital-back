package com.example.espelhovitalback.repository;


import com.example.espelhovitalback.model.Consulta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.*;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
    long countByUsuarioId(Long usuarioId);

    List<Consulta> findByUsuarioIdAndDataAgendadaBetweenOrderByDataAgendadaAscIdAsc(Long usuarioId, java.time.LocalDate start, java.time.LocalDate end);

    void deleteByUsuarioId(Long usuarioId);

    org.springframework.data.domain.Page<Consulta> findByUsuarioId(Long usuarioId, org.springframework.data.domain.Pageable pageable);

    List<Consulta> findByUsuarioIdOrderByDataAgendadaAscHoraAgendadaAscIdAsc(Long usuarioId);

    List<Consulta> findByUsuarioIdAndStatusAndDataAgendadaGreaterThanEqualOrderByDataAgendadaAscHoraAgendadaAsc(Long usuarioId, String status, LocalDate data);

    Optional<Consulta> findByIdAndUsuarioId(Long id, Long usuarioId);
}
