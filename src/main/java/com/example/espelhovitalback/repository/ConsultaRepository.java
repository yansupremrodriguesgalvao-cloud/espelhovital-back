package com.example.espelhovitalback.repository;

import com.example.espelhovitalback.model.Consulta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    List<Consulta> findAllByOrderByDataAgendadaAscHoraAgendadaAsc();

    List<Consulta> findByDataAgendadaGreaterThanEqualAndStatusOrderByDataAgendadaAsc(
            LocalDate hoje, String status);

    List<Consulta> findByUsuarioIdOrderByDataAgendadaAsc(Long usuarioId);
}
