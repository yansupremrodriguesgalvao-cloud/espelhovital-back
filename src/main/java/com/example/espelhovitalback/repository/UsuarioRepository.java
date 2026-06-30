package com.example.espelhovitalback.repository;

import com.example.espelhovitalback.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}