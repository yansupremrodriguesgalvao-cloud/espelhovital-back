package com.example.espelhovitalback.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "humor")
public class Humor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ex: "😊 Feliz, ✨ Animada" (emoções selecionadas, separadas por vírgula) */
    private String estado;

    @Column(length = 500)
    private String observacao;

    @Column(name = "data_registro")
    private LocalDate dataRegistro;

    public Humor() {
    }

    @PrePersist
    public void prePersist() {
        this.dataRegistro = LocalDate.now();
    }

    public Long getId() {
        return id;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public LocalDate getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDate dataRegistro) {
        this.dataRegistro = dataRegistro;
    }
}
