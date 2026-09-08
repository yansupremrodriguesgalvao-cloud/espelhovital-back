package com.example.espelhovitalback.service;

import com.example.espelhovitalback.model.CicloMenstrual;
import com.example.espelhovitalback.model.Humor;
import com.example.espelhovitalback.model.Sono;
import com.example.espelhovitalback.repository.CicloMenstrualRepository;
import com.example.espelhovitalback.repository.HumorRepository;
import com.example.espelhovitalback.repository.SonoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Gera recomendações personalizadas combinando os dados de ciclo menstrual,
 * sono e humor já registrados pela usuária. As regras são baseadas em boas
 * práticas de saúde geral e não substituem orientação médica.
 */
@Service
public class RecomendacaoService {

    @Autowired
    private CicloMenstrualRepository cicloRepository;

    @Autowired
    private SonoRepository sonoRepository;

    @Autowired
    private HumorRepository humorRepository;

    public List<String> gerarRecomendacoes() {

        List<String> recomendacoes = new ArrayList<>();
        LocalDate hoje = LocalDate.now();

        // ---------- Baseadas no ciclo menstrual ----------
        List<CicloMenstrual> ciclos = cicloRepository.findAll();
        if (!ciclos.isEmpty()) {
            CicloMenstrual ultimo = ciclos.get(ciclos.size() - 1);

            boolean emMenstruacao = ultimo.getDataUltimaMenstruacao() != null
                    && !hoje.isBefore(ultimo.getDataUltimaMenstruacao())
                    && hoje.isBefore(ultimo.getDataUltimaMenstruacao()
                            .plusDays(ultimo.getDuracaoMenstruacao() != null ? ultimo.getDuracaoMenstruacao() : 5));

            boolean emPeriodoFertil = ultimo.getInicioPeriodoFertil() != null && ultimo.getFimPeriodoFertil() != null
                    && !hoje.isBefore(ultimo.getInicioPeriodoFertil())
                    && !hoje.isAfter(ultimo.getFimPeriodoFertil());

            if (emMenstruacao) {
                recomendacoes.add("🩸 Você está no período menstrual: priorize alimentos ricos em ferro (folhas verde-escuras, feijão, carnes magras) e mantenha-se hidratada.");
                recomendacoes.add("🧘 Exercícios leves como caminhada ou yoga ajudam a reduzir cólicas nesta fase.");
            } else if (emPeriodoFertil) {
                recomendacoes.add("🌱 Você está no período fértil. Se for do seu planejamento familiar, este é o momento de maior atenção ao ciclo.");
            } else if (ultimo.getProximaMenstruacao() != null) {
                long diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(hoje, ultimo.getProximaMenstruacao());
                if (diasRestantes >= 0 && diasRestantes <= 5) {
                    recomendacoes.add("🌙 Sua próxima menstruação está próxima. É comum sentir TPM: durma bem e reduza cafeína e sal.");
                }
            }
        } else {
            recomendacoes.add("📌 Registre seu ciclo menstrual para receber recomendações personalizadas sobre suas fases.");
        }

        // ---------- Baseadas no sono ----------
        List<Sono> registrosSono = sonoRepository.findAll();
        if (!registrosSono.isEmpty()) {
            double media = registrosSono.stream()
                    .filter(s -> s.getHoras() != null)
                    .mapToInt(Sono::getHoras)
                    .average()
                    .orElse(0);

            if (media < 6) {
                recomendacoes.add("😴 Sua média de sono está em " + arredondar(media) + "h, abaixo do ideal (7-9h). Tente manter um horário fixo para dormir.");
            } else if (media > 9) {
                recomendacoes.add("⚠️ Você tem dormido mais de 9h em média. Excesso de sono também pode indicar cansaço ou desânimo — vale observar.");
            } else {
                recomendacoes.add("✅ Sua média de sono (" + arredondar(media) + "h) está dentro da faixa saudável. Continue assim!");
            }
        }

        // ---------- Baseadas no humor ----------
        List<Humor> registrosHumor = humorRepository.findAll();
        if (!registrosHumor.isEmpty()) {
            Humor ultimo = registrosHumor.get(registrosHumor.size() - 1);
            String estado = ultimo.getEstado() != null ? ultimo.getEstado().toLowerCase() : "";

            if (estado.contains("triste") || estado.contains("ansios") || estado.contains("cansad")) {
                recomendacoes.add("💜 Notamos que seu último humor foi \"" + ultimo.getEstado() + "\". Pequenas pausas, respiração consciente e conversar com alguém de confiança podem ajudar.");
            } else if (estado.contains("feliz") || estado.contains("anima")) {
                recomendacoes.add("😊 Seu humor recente foi positivo! Aproveite para manter as rotinas que estão funcionando bem para você.");
            }
        }

        if (recomendacoes.isEmpty()) {
            recomendacoes.add("📌 Comece registrando seu ciclo, sono e humor para receber recomendações personalizadas.");
        }

        return recomendacoes;
    }

    private double arredondar(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
