package com.example.espelhovitalback.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class ConsultaResumoDTO {

    private Long id;
    private String tipo;
    private String titulo;
    private LocalDate dataAgendada;
    private LocalTime horaAgendada;
    private Long diasRestantes;

    public ConsultaResumoDTO() {
    }

    public ConsultaResumoDTO(Long id, String tipo, String titulo, LocalDate dataAgendada,
                              LocalTime horaAgendada, Long diasRestantes) {
        this.id = id;
        this.tipo = tipo;
        this.titulo = titulo;
        this.dataAgendada = dataAgendada;
        this.horaAgendada = horaAgendada;
        this.diasRestantes = diasRestantes;
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

    public Long getDiasRestantes() {
        return diasRestantes;
    }

    public void setDiasRestantes(Long diasRestantes) {
        this.diasRestantes = diasRestantes;
    }
}
