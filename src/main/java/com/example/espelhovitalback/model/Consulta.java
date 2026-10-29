package com.example.espelhovitalback.model;

import jakarta.validation.constraints.*;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "consulta")
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private Long id;

    /**
     * CONSULTA ou EXAME
     */
    @Column(nullable = false)
    @NotBlank
    @Pattern(regexp = "CONSULTA|EXAME")
    private String tipo;

    /**
     * Ex: "Consulta com ginecologista", "Exame de sangue"
     */
    @Column(nullable = false)
    @NotBlank
    @Size(max = 200)
    private String titulo;

    @Size(max = 255)
    private String local;

    @Column(name = "profissional")
    @Size(max = 255)
    private String profissional;

    @Column(name = "data_agendada", nullable = false)
    @NotNull
    private LocalDate dataAgendada;

    @Column(name = "hora_agendada")
    private LocalTime horaAgendada;

    @Column(length = 500)
    @Size(max = 500)
    private String observacao;

    /**
     * AGENDADA, REALIZADA ou CANCELADA
     */
    @Column(nullable = false)
    @Pattern(regexp = "AGENDADA|REALIZADA|CANCELADA")
    private String status;

    /**
     * Quantos dias antes o lembrete deve avisar (padrão 1 dia)
     */
    @Column(name = "lembrete_dias_antes")
    @Min(0)
    @Max(365)
    private Integer lembreteDiasAntes;

    /**
     * Vincula o compromisso ao usuário dono (opcional, mantém compatibilidade se nulo)
     */
    @Column(name = "usuario_id")
    private Long usuarioId;

    public Consulta() {
    }

    @PrePersist
    public void prePersist() {
        if (this.status == null || this.status.isBlank()) {
            this.status = "AGENDADA";
        }
        if (this.lembreteDiasAntes == null) {
            this.lembreteDiasAntes = 1;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public String getProfissional() {
        return profissional;
    }

    public void setProfissional(String profissional) {
        this.profissional = profissional;
    }

    public LocalDate getDataAgendada() {
        return dataAgendada;
    }

    public void setDataAgendada(LocalDate dataAgendada) {
        this.dataAgendada = dataAgendada;
    }

    public LocalTime getHoraAgendada() {
        return horaAgendada;
    }

    public void setHoraAgendada(LocalTime horaAgendada) {
        this.horaAgendada = horaAgendada;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getLembreteDiasAntes() {
        return lembreteDiasAntes;
    }

    public void setLembreteDiasAntes(Integer lembreteDiasAntes) {
        this.lembreteDiasAntes = lembreteDiasAntes;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }
}
