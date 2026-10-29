package com.example.espelhovitalback.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "diario")
@Getter
@Setter
public class Diario {
    public enum Fluxo {NAO_INFORMADO, SEM_FLUXO, ESCAPE, LEVE, MODERADO, INTENSO}

    public enum Sintoma {
        COLICA("Cólica"), DOR_CABECA("Dor de cabeça"), INCHACO("Inchaço"),
        SENSIBILIDADE_MAMAS("Sensibilidade nas mamas"), CANSACO("Cansaço"),
        NAUSEA("Náusea"), ACNE("Acne"), DOR_LOMBAR("Dor lombar"),
        ALTERACAO_APETITE("Alteração de apetite"), TONTURA("Tontura"),
        ALTERACAO_INTESTINAL("Alteração intestinal"), OUTRO("Outro");
        private final String descricao;

        Sintoma(String descricao) {
            this.descricao = descricao;
        }

        public String descricao() {
            return descricao;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long usuarioId;
    @Column(nullable = false)
    private LocalDate dataRegistro;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Fluxo fluxo;
    private Integer intensidadeDor;
    @Column(nullable = false, length = 500)
    private String sintomas = "";
    @Column(length = 2000)
    private String observacoes;
    private Long sonoId;
    private Long humorId;
    @Column(nullable = false)
    private Instant atualizadoEm;
}
