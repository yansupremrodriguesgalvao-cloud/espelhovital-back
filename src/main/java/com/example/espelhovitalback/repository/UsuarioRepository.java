package com.example.espelhovitalback.repository;

import com.example.espelhovitalback.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> lockById(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<Usuario> findByEmailIgnoreCase(String email);
}