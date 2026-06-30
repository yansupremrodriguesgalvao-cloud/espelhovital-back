package com.example.espelhovitalback.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "humor")
public class Humor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String humor;

    private LocalDate dataRegistro;

    public Humor() {
    }

    public Long getId() {
        return id;
    }

    public String getHumor() {
        return humor;
    }

    public void setHumor(String humor) {
        this.humor = humor;
    }

    public LocalDate getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDate dataRegistro) {
        this.dataRegistro = dataRegistro;
    }
}