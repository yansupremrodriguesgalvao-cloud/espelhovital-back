package com.example.espelhovitalback.service;

import com.example.espelhovitalback.dto.ConsultaResumoDTO;
import com.example.espelhovitalback.dto.DashboardDTO;
import com.example.espelhovitalback.model.CicloMenstrual;
import com.example.espelhovitalback.model.Consulta;
import com.example.espelhovitalback.model.Humor;
import com.example.espelhovitalback.model.Sono;
import com.example.espelhovitalback.repository.CicloMenstrualRepository;
import com.example.espelhovitalback.repository.HumorRepository;
import com.example.espelhovitalback.repository.SonoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    @Autowired
    private CicloMenstrualRepository cicloRepository;

    @Autowired
    private SonoRepository sonoRepository;

    @Autowired
    private HumorRepository humorRepository;

    @Autowired
    private ConsultaService consultaService;

    public DashboardDTO montar(Long usuarioId) {

        DashboardDTO dto = new DashboardDTO();
        LocalDate hoje = LocalDate.now();
        List<String> alertas = new ArrayList<>();

        // ---------- Ciclo menstrual ----------
        List<CicloMenstrual> ciclos = usuarioId != null
                ? cicloRepository.findByUsuarioId(usuarioId)
                : cicloRepository.findAll();
        if (!ciclos.isEmpty()) {
            CicloMenstrual ultimo = ciclos.get(ciclos.size() - 1);

            dto.setProximaMenstruacao(ultimo.getProximaMenstruacao());
            dto.setOvulacao(ultimo.getOvulacao());
            dto.setInicioPeriodoFertil(ultimo.getInicioPeriodoFertil());
            dto.setFimPeriodoFertil(ultimo.getFimPeriodoFertil());

            if (ultimo.getProximaMenstruacao() != null) {
                long dias = ChronoUnit.DAYS.between(hoje, ultimo.getProximaMenstruacao());
                dto.setDiasParaProximaMenstruacao((int) dias);

                if (dias <= 2 && dias >= 0) {
                    alertas.add("🌸 Sua menstruação deve começar em " + dias + " dia(s).");
                }
            }

            dto.setFaseCicloAtual(calcularFase(hoje, ultimo));
        }

        // ---------- Sono ----------
        List<Sono> registrosSono = usuarioId != null
                ? sonoRepository.findByUsuarioId(usuarioId)
                : sonoRepository.findAll();
        dto.setTotalRegistrosSono(registrosSono.size());
        if (!registrosSono.isEmpty()) {
            double media = registrosSono.stream()
                    .filter(s -> s.getHoras() != null)
                    .mapToInt(Sono::getHoras)
                    .average()
                    .orElse(0);
            dto.setMediaHorasSono(Math.round(media * 10.0) / 10.0);
            dto.setUltimaQualidadeSono(registrosSono.get(registrosSono.size() - 1).getQualidade());

            if (media < 6) {
                alertas.add("😴 Sua média de sono está abaixo do recomendado.");
            }
        }

        // ---------- Humor ----------
        List<Humor> registrosHumor = usuarioId != null
                ? humorRepository.findByUsuarioId(usuarioId)
                : humorRepository.findAll();
        if (!registrosHumor.isEmpty()) {
            Humor ultimo = registrosHumor.get(registrosHumor.size() - 1);
            dto.setUltimoHumor(ultimo.getEstado());
            dto.setDataUltimoHumor(ultimo.getDataRegistro());
        }

        // ---------- Consultas / exames ----------
        List<Consulta> proximas = consultaService.proximos();
        dto.setTotalConsultasProximas(proximas.size());

        List<ConsultaResumoDTO> resumo = new ArrayList<>();
        for (Consulta c : proximas) {
            long dias = ChronoUnit.DAYS.between(hoje, c.getDataAgendada());
            resumo.add(new ConsultaResumoDTO(
                    c.getId(), c.getTipo(), c.getTitulo(), c.getDataAgendada(),
                    c.getHoraAgendada(), dias
            ));

            Integer diasAntes = c.getLembreteDiasAntes() != null ? c.getLembreteDiasAntes() : 1;
            if (dias >= 0 && dias <= diasAntes) {
                String rotulo = "EXAME".equalsIgnoreCase(c.getTipo()) ? "🧪 Exame" : "🩺 Consulta";
                alertas.add(rotulo + " \"" + c.getTitulo() + "\" em " + dias + " dia(s).");
            }
        }
        dto.setProximasConsultas(resumo.size() > 5 ? resumo.subList(0, 5) : resumo);

        dto.setAlertas(alertas);

        return dto;
    }

    private String calcularFase(LocalDate hoje, CicloMenstrual ciclo) {

        if (ciclo.getDataUltimaMenstruacao() == null) return "Indefinida";

        LocalDate fimMenstruacao = ciclo.getDataUltimaMenstruacao()
                .plusDays(ciclo.getDuracaoMenstruacao() != null ? ciclo.getDuracaoMenstruacao() : 5);

        if (!hoje.isBefore(ciclo.getDataUltimaMenstruacao()) && hoje.isBefore(fimMenstruacao)) {
            return "🩸 Fase Menstrual";
        }

        if (ciclo.getInicioPeriodoFertil() != null && ciclo.getFimPeriodoFertil() != null
                && !hoje.isBefore(ciclo.getInicioPeriodoFertil()) && !hoje.isAfter(ciclo.getFimPeriodoFertil())) {
            return "🌱 Período Fértil";
        }

        if (ciclo.getOvulacao() != null && hoje.isEqual(ciclo.getOvulacao())) {
            return "🥚 Ovulação";
        }

        if (ciclo.getProximaMenstruacao() != null && hoje.isBefore(ciclo.getProximaMenstruacao())) {
            return "🌙 Fase Lútea";
        }

        return "🌸 Fase Folicular";
    }
}
