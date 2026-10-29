package com.example.espelhovitalback.dto;

import jakarta.validation.constraints.*;

import java.time.*;

import com.example.espelhovitalback.model.Consulta;

public record ConsultaRequest(@NotBlank @Pattern(regexp = "CONSULTA|EXAME") String tipo,
                              @NotBlank @Size(max = 200) String titulo, @Size(max = 255) String local,
                              @Size(max = 255) String profissional,
                              @NotNull LocalDate dataAgendada, LocalTime horaAgendada,
                              @Size(max = 500) String observacao,
                              @Pattern(regexp = "AGENDADA|REALIZADA|CANCELADA") String status,
                              @Min(0) @Max(365) Integer lembreteDiasAntes) {
    public Consulta toEntity() {
        Consulta c = new Consulta();
        c.setTipo(tipo);
        c.setTitulo(titulo);
        c.setLocal(local);
        c.setProfissional(profissional);
        c.setDataAgendada(dataAgendada);
        c.setHoraAgendada(horaAgendada);
        c.setObservacao(observacao);
        c.setStatus(status);
        c.setLembreteDiasAntes(lembreteDiasAntes);
        return c;
    }
}
