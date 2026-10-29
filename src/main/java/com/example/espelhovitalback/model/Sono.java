package com.example.espelhovitalback.model;

import jakarta.validation.constraints.*;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "sono")
public class Sono {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @NotNull
    @Min(0)
    @Max(24)
    private Double horas;

    @NotBlank
    @Pattern(regexp = "Excelente|Boa|Regular|Ruim")
    private String qualidade;

    @Column(name = "data_registro")
    private LocalDate dataRegistro;

    public Sono() {
    }

    @PrePersist
    public void prePersist() {
        if (this.dataRegistro == null) this.dataRegistro = LocalDate.now();
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

    public Double getHoras() {
        return horas;
    }

    public void setHoras(Double horas) {
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
