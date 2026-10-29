package com.example.espelhovitalback.repository;

import com.example.espelhovitalback.model.LembretePreferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;

import java.util.Optional;

public interface LembretePreferenciaRepository extends JpaRepository<LembretePreferencia, Long> {
    Optional<LembretePreferencia> findByUsuarioId(Long usuarioId);

    Page<LembretePreferencia> findByAtivoTrueAndEmailAtivoTrue(Pageable pageable);

    void deleteByUsuarioId(Long usuarioId);
}
