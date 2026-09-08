package com.example.espelhovitalback.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "sono")
public class Sono {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    private Integer horas;

    private String qualidade;

    @Column(name = "data_registro")
    private LocalDate dataRegistro;

    public Sono() {}

    @PrePersist
    public void prePersist() {
        this.dataRegistro = LocalDate.now();
    }

    // GETTERS E SETTERS

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Integer getHoras() {
        return horas;
    }

    public void setHoras(Integer horas) {
        this.horas = horas;
    }

    public String getQualidade() {
        return qualidade;
    }

    public void setQualidade(String qualidade) {
        this.qualidade = qualidade;
    }

    public LocalDate getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDate dataRegistro) {
        this.dataRegistro = dataRegistro;
    }
}