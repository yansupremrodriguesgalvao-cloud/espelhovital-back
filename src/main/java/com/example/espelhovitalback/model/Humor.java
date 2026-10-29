package com.example.espelhovitalback.model;

import jakarta.validation.constraints.*;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "humor")
public class Humor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    /**
     * Ex: "😊 Feliz, ✨ Animada" (emoções selecionadas, separadas por vírgula)
     */
    @NotBlank
    @Size(max = 255)
    private String estado;

    @Column(length = 500)
    @Size(max = 500)
    private String observacao;

    @Column(name = "data_registro")
    private LocalDate dataRegistro;

    public Humor() {
    }

    @PrePersist
    public void prePersist() {
        if (this.dataRegistro == null) this.dataRegistro = LocalDate.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
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
