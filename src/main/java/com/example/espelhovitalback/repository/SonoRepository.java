package com.example.espelhovitalback.repository;

import com.example.espelhovitalback.model.Sono;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SonoRepository extends JpaRepository<Sono, Long> {
    List<Sono> findByUsuarioId(Long usuarioId);
}
