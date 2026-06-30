package com.example.espelhovitalback.repository;

import com.example.espelhovitalback.model.Humor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HumorRepository extends JpaRepository<Humor, Long> {
}