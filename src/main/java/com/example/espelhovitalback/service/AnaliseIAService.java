package com.example.espelhovitalback.service;

import com.example.espelhovitalback.model.CicloMenstrual;
import com.example.espelhovitalback.model.Humor;
import com.example.espelhovitalback.model.Sono;
import com.example.espelhovitalback.repository.CicloMenstrualRepository;
import com.example.espelhovitalback.repository.HumorRepository;
import com.example.espelhovitalback.repository.SonoRepository;

import org.springframework.stereotype.Service;
import com.example.espelhovitalback.config.CurrentUser;

import java.util.*;

/**
 * Motor de análise de dados que cruza sono, humor e ciclo menstrual para gerar
 * insights automáticos (estatísticas + regras). Serve de base para a tela
 * "Inteligência Artificial para Análise de Dados". Não é um modelo de machine
 * learning treinado — é um motor estatístico/heurístico que processa os
 * registros da usuária e resume padrões relevantes, no mesmo espírito da
 * análise de sono que já existia no projeto.
 */
@Service
public class AnaliseIAService {
    public AnaliseIAService(CurrentUser current, SonoRepository sonoRepository, HumorRepository humorRepository, CicloMenstrualRepository cicloRepository) {
        this.current = current;
        this.sonoRepository = sonoRepository;
        this.humorRepository = humorRepository;
        this.cicloRepository = cicloRepository;
    }


    private final CurrentUser current;

    private final SonoRepository sonoRepository;

    private final HumorRepository humorRepository;

    private final CicloMenstrualRepository cicloRepository;

    public Map<String, Object> analisar() {

        Map<String, Object> resultado = new LinkedHashMap<>();
        List<String> insights = new ArrayList<>();

        List<Sono> sonos = sonoRepository.findByUsuarioIdOrderByDataRegistroAscIdAsc(current.id());
        List<Humor> humores = humorRepository.findByUsuarioIdOrderByDataRegistroAscIdAsc(current.id());
        List<CicloMenstrual> ciclos = cicloRepository.findByUsuarioIdOrderByDataUltimaMenstruacaoAscIdAsc(current.id());

        // ---------- Estatísticas de sono ----------
        double mediaSono = sonos.stream()
                .filter(s -> s.getHoras() != null)
                .mapToDouble(Sono::getHoras)
                .average().orElse(0);

        resultado.put("mediaHorasSono", arredondar(mediaSono));
        resultado.put("totalRegistrosSono", sonos.size());

        if (!sonos.isEmpty()) {
            long noitesRuins = sonos.stream()
                    .filter(s -> "Ruim".equalsIgnoreCase(s.getQualidade()))
                    .count();

            double percentualRuim = (noitesRuins * 100.0) / sonos.size();
            resultado.put("percentualNoitesRuins", arredondar(percentualRuim));

            if (percentualRuim >= 40) {
                insights.add("📊 " + arredondar(percentualRuim) + "% das suas noites registradas foram classificadas como \"Ruim\". Vale investigar rotina antes de dormir, uso de telas e horários irregulares.");
            }

            // tendência: compara a média da segunda metade dos registros com a primeira
            if (sonos.size() >= 4) {
                int meio = sonos.size() / 2;
                double mediaPrimeira = sonos.subList(0, meio).stream()
                        .filter(s -> s.getHoras() != null).mapToDouble(Sono::getHoras).average().orElse(0);
                double mediaSegunda = sonos.subList(meio, sonos.size()).stream()
                        .filter(s -> s.getHoras() != null).mapToDouble(Sono::getHoras).average().orElse(0);

                if (mediaSegunda - mediaPrimeira >= 0.8) {
                    insights.add("📈 Sua média de sono vem melhorando ao longo dos registros mais recentes.");
                } else if (mediaPrimeira - mediaSegunda >= 0.8) {
                    insights.add("📉 Sua média de sono caiu nos registros mais recentes em comparação aos anteriores.");
                }
            }
        }

        // ---------- Estatísticas de humor ----------
        Map<String, Long> contagemHumor = new LinkedHashMap<>();
        for (Humor h : humores) {
            if (h.getEstado() == null) continue;
            for (String parte : h.getEstado().split(",")) {
                String chave = parte.trim();
                if (chave.isEmpty()) continue;
                contagemHumor.merge(chave, 1L, Long::sum);
            }
        }
        resultado.put("distribuicaoHumor", contagemHumor);
        resultado.put("totalRegistrosHumor", humores.size());

        if (!contagemHumor.isEmpty()) {
            String maisFrequente = Collections.max(contagemHumor.entrySet(), Map.Entry.comparingByValue()).getKey();
            insights.add("🧠 O estado emocional mais registrado até agora foi \"" + maisFrequente + "\".");
        }

        // ---------- Correlação simples: sono baixo x humor negativo no mesmo dia ----------
        Set<java.time.LocalDate> diasHumorNegativo = new HashSet<>();
        for (Humor h : humores) {
            if (h.getDataRegistro() == null || h.getEstado() == null) continue;
            String estado = h.getEstado().toLowerCase(Locale.ROOT);
            if (estado.contains("triste") || estado.contains("ansios") || estado.contains("cansad") || estado.contains("irritad"))
                diasHumorNegativo.add(h.getDataRegistro());
        }
        long diasComSonoBaixoEHumorNegativo = sonos.stream()
                .filter(s -> s.getHoras() != null && s.getHoras() < 6 && diasHumorNegativo.contains(s.getDataRegistro()))
                .map(Sono::getDataRegistro).distinct().count();

        if (diasComSonoBaixoEHumorNegativo >= 2) {
            insights.add("🔗 Identificamos " + diasComSonoBaixoEHumorNegativo + " dia(s) em que dormir menos de 6h coincidiu com um humor mais negativo. Cuidar do sono pode ajudar seu bem-estar emocional.");
        }

        // ---------- Ciclo menstrual: regularidade ----------
        if (!ciclos.isEmpty()) {
            CicloMenstrual ultimo = ciclos.get(ciclos.size() - 1);
            if (ultimo.getDuracaoCiclo() != null) {
                if (ultimo.getDuracaoCiclo() < 21 || ultimo.getDuracaoCiclo() > 35) {
                    insights.add("🌸 A duração do seu ciclo informada (" + ultimo.getDuracaoCiclo() + " dias) está fora da faixa mais comum (21-35 dias). Considere conversar com um profissional de saúde.");
                } else {
                    insights.add("🌸 A duração do seu ciclo (" + ultimo.getDuracaoCiclo() + " dias) está na faixa informada; esse dado isolado não determina regularidade.");
                }
            }
        }

        if (insights.isEmpty()) {
            insights.add("📌 Continue registrando seus dados de sono, humor e ciclo para desbloquear análises mais completas.");
        }

        resultado.put("insights", insights);
        return resultado;
    }

    private double arredondar(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
