package com.example.espelhovitalback.repository;

import com.example.espelhovitalback.model.CicloMenstrual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CicloMenstrualRepository extends JpaRepository<CicloMenstrual, Long> {

}