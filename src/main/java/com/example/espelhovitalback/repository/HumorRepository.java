package com.example.espelhovitalback.repository;

import com.example.espelhovitalback.model.Humor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HumorRepository extends JpaRepository<Humor, Long> {
    List<Humor> findByUsuarioId(Long usuarioId);
}
