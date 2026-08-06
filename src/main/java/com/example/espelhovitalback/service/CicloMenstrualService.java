package com.example.espelhovitalback.service;

import com.example.espelhovitalback.dto.CicloRequest;
import com.example.espelhovitalback.dto.CicloResponse;
import com.example.espelhovitalback.model.CicloMenstrual;
import com.example.espelhovitalback.repository.CicloMenstrualRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class CicloMenstrualService {

    @Autowired
    private CicloMenstrualRepository repository;

    public CicloResponse calcularCiclo(CicloRequest request) {

        LocalDate proximaMenstruacao =
                request.getDataUltimaMenstruacao()
                        .plusDays(request.getDuracaoCiclo());

        LocalDate ovulacao =
                proximaMenstruacao.minusDays(14);

        LocalDate inicioPeriodoFertil =
                ovulacao.minusDays(5);

        LocalDate fimPeriodoFertil =
                ovulacao.plusDays(1);

        CicloMenstrual ciclo = new CicloMenstrual();

        ciclo.setDataUltimaMenstruacao(
                request.getDataUltimaMenstruacao());

        ciclo.setDuracaoCiclo(
                request.getDuracaoCiclo());

        ciclo.setDuracaoMenstruacao(
                request.getDuracaoMenstruacao());

        ciclo.setProximaMenstruacao(
                proximaMenstruacao);

        ciclo.setOvulacao(
                ovulacao);

        ciclo.setInicioPeriodoFertil(
                inicioPeriodoFertil);

        ciclo.setFimPeriodoFertil(
                fimPeriodoFertil);

        repository.save(ciclo);

        return new CicloResponse(
                proximaMenstruacao,
                ovulacao,
                inicioPeriodoFertil,
                fimPeriodoFertil
        );
    }
}
