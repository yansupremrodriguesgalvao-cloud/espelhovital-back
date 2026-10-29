package com.example.espelhovitalback.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * DTO usado para consulta e atualização do perfil.
 * Nunca inclui a senha, para não expor o hash pro front-end.
 */
public class PerfilDTO {

    private Long id;
    @Size(min = 3, max = 150)
    private String nome;
    @Email
    @Size(max = 254)
    private String email;
    @Size(max = 30)
    private String telefone;
    @Past
    private LocalDate dataNascimento;
    @Size(max = 150)
    private String cidade;
    @Positive
    @Max(700)
    private Double peso;
    @Positive
    @Max(300)
    private Double altura;
    @Size(max = 255)
    private String objetivo;

    public PerfilDTO() {
    }

    public PerfilDTO(Long id, String nome, String email, String telefone,
                     LocalDate dataNascimento, String cidade,
                     Double peso, Double altura, String objetivo) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.dataNascimento = dataNascimento;
        this.cidade = cidade;
        this.peso = peso;
        this.altura = altura;
        this.objetivo = objetivo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public Double getPeso() {
        return peso;
    }

    public void setPeso(Double peso) {
        this.peso = peso;
    }

    public Double getAltura() {
        return altura;
    }

    public void setAltura(Double altura) {
        this.altura = altura;
    }

    public String getObjetivo() {
        return objetivo;
    }

    public void setObjetivo(String objetivo) {
        this.objetivo = objetivo;
    }
}
