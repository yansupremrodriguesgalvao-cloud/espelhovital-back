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

import org.springframework.stereotype.Service;
import com.example.espelhovitalback.config.CurrentUser;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {
    public DashboardService(CurrentUser current, CicloMenstrualRepository cicloRepository, SonoRepository sonoRepository, HumorRepository humorRepository, ConsultaService consultaService, CycleProjection projection) {
        this.projection = projection;
        this.current = current;
        this.cicloRepository = cicloRepository;
        this.sonoRepository = sonoRepository;
        this.humorRepository = humorRepository;
        this.consultaService = consultaService;
    }


    private final CurrentUser current;
    private final CycleProjection projection;

    private final CicloMenstrualRepository cicloRepository;

    private final SonoRepository sonoRepository;

    private final HumorRepository humorRepository;

    private final ConsultaService consultaService;

    public DashboardDTO montar(Long usuarioId) {

        if (usuarioId != null) current.requireOwner(usuarioId);
        DashboardDTO dto = new DashboardDTO();
        LocalDate hoje = LocalDate.now();
        List<String> alertas = new ArrayList<>();

        // ---------- Ciclo menstrual ----------
        List<CicloMenstrual> ciclos = cicloRepository.findByUsuarioIdOrderByDataUltimaMenstruacaoAscIdAsc(current.id());
        if (!ciclos.isEmpty()) {
            CicloMenstrual ultimo = ciclos.get(ciclos.size() - 1);

            var calendario = projection.project(ciclos, hoje);
            calendario.previsoes().stream().filter(p -> !p.inicioMaisTarde().isBefore(hoje)).findFirst().ifPresent(p -> {
                dto.setProximaMenstruacao(p.inicio());
                dto.setOvulacao(p.ovulacao());
                dto.setInicioPeriodoFertil(p.inicioPeriodoFertil());
                dto.setFimPeriodoFertil(p.fimPeriodoFertil());
                if (calendario.irregular()) {
                    dto.setProximaMenstruacaoMinima(p.inicioMaisCedo());
                    dto.setProximaMenstruacaoMaxima(p.inicioMaisTarde());
                }
            });

            if (dto.getProximaMenstruacao() != null) {
                long dias = ChronoUnit.DAYS.between(hoje, dto.getProximaMenstruacao());
                dto.setDiasParaProximaMenstruacao((int) dias);

                if (dias <= 2 && dias >= 0 && !calendario.irregular()) {
                    alertas.add("🌸 Sua menstruação está estimada para daqui a " + dias + " dia(s).");
                }
            }

            dto.setFaseCicloAtual(calcularFase(hoje, ultimo));
            if (calendario.irregular() && !"Menstruação registrada".equals(dto.getFaseCicloAtual()))
                dto.setFaseCicloAtual("Ciclo irregular — consulte o calendário");
        }

        // ---------- Sono ----------
        List<Sono> registrosSono = sonoRepository.findByUsuarioIdOrderByDataRegistroAscIdAsc(current.id());
        dto.setTotalRegistrosSono(registrosSono.size());
        if (!registrosSono.isEmpty()) {
            double media = registrosSono.stream()
                    .filter(s -> s.getHoras() != null)
                    .mapToDouble(Sono::getHoras)
                    .average()
                    .orElse(0);
            dto.setMediaHorasSono(Math.round(media * 10.0) / 10.0);
            dto.setUltimaQualidadeSono(registrosSono.get(registrosSono.size() - 1).getQualidade());

            if (media < 6) {
                alertas.add("😴 Sua média de sono está abaixo do recomendado.");
            }
        }

        // ---------- Humor ----------
        List<Humor> registrosHumor = humorRepository.findByUsuarioIdOrderByDataRegistroAscIdAsc(current.id());
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

        if (ciclo.getDataUltimaMenstruacao() == null || hoje.isBefore(ciclo.getDataUltimaMenstruacao()))
            return "Sem dados para esta data";
        LocalDate fim = ciclo.getDataUltimaMenstruacao().plusDays(ciclo.getDuracaoMenstruacao() == null ? 5 : ciclo.getDuracaoMenstruacao());
        if (hoje.isBefore(fim)) return "Menstruação registrada";
        if (Boolean.TRUE.equals(ciclo.getIrregular())) return "Ciclo irregular — consulte o calendário";
        if (ciclo.getProximaMenstruacao() == null || !hoje.isBefore(ciclo.getProximaMenstruacao()))
            return "Atualize o início da menstruação";
        if (ciclo.getOvulacao() != null && hoje.equals(ciclo.getOvulacao())) return "Ovulação estimada";
        if (ciclo.getInicioPeriodoFertil() != null && ciclo.getFimPeriodoFertil() != null && !hoje.isBefore(ciclo.getInicioPeriodoFertil()) && !hoje.isAfter(ciclo.getFimPeriodoFertil()))
            return "Período fértil estimado";
        if (ciclo.getOvulacao() != null && hoje.isAfter(ciclo.getOvulacao())) return "Fase lútea estimada";
        return "Fase folicular estimada";
    }
}
