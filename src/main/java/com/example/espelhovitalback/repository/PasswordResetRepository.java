package com.example.espelhovitalback.repository;


import com.example.espelhovitalback.model.PasswordReset;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    void deleteByUsuarioId(Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordReset> findByTokenHash(String hash);

    Optional<PasswordReset> findByUsuarioId(Long id);
}
