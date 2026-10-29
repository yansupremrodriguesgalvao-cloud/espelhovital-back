package com.example.espelhovitalback.service;

import com.example.espelhovitalback.config.CurrentUser;
import com.example.espelhovitalback.dto.LembreteConfig;
import com.example.espelhovitalback.dto.LembreteConfig.Preferences;
import com.example.espelhovitalback.model.*;
import com.example.espelhovitalback.repository.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class LembreteService {
    public record Notice(String chave, String titulo, String descricao, String caminho) {
    }

    private final LembretePreferenciaRepository preferences;
    private final LembreteEntregaRepository deliveries;
    private final UsuarioRepository users;
    private final DiarioRepository diary;
    private final SonoRepository sleeps;
    private final ConsultaRepository appointments;
    private final CicloMenstrualRepository cycles;
    private final CycleProjection projection;
    private final CurrentUser current;
    private final Clock clock;
    private final ObjectProvider<JavaMailSender> mail;
    private final String host, from, frontend;

    public LembreteService(LembretePreferenciaRepository preferences, LembreteEntregaRepository deliveries, UsuarioRepository users,
                           DiarioRepository diary, SonoRepository sleeps, ConsultaRepository appointments, CicloMenstrualRepository cycles,
                           CycleProjection projection, CurrentUser current, Clock clock, ObjectProvider<JavaMailSender> mail,
                           @Value("${spring.mail.host:}") String host, @Value("${app.mail.from:noreply@espelhovital.local}") String from,
                           @Value("${app.frontend-url:http://localhost:5500}") String frontend) {
        this.preferences = preferences;
        this.deliveries = deliveries;
        this.users = users;
        this.diary = diary;
        this.sleeps = sleeps;
        this.appointments = appointments;
        this.cycles = cycles;
        this.projection = projection;
        this.current = current;
        this.clock = clock;
        this.mail = mail;
        this.host = host;
        this.from = from;
        this.frontend = frontend;
    }

    public boolean emailAvailable() {
        return !host.isBlank() && mail.getIfAvailable() != null;
    }

    public LembreteConfig config() {
        return new LembreteConfig(toDto(preferences.findByUsuarioId(current.id()).orElseGet(LembretePreferencia::new)), emailAvailable());
    }

    public LembreteConfig save(Preferences body) {
        try {
            ZoneId.of(body.fusoHorario());
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Escolha um fuso horário válido.");
        }
        if (body.ativo() && !(body.diario() || body.sono() || body.consultas() || body.ciclo()))
            throw new IllegalArgumentException("Escolha ao menos um tipo de lembrete.");
        if (body.emailAtivo() && !emailAvailable())
            throw new IllegalArgumentException("O envio por e-mail ainda não está configurado neste ambiente.");
        Long owner = current.id();
        users.lockById(owner).orElseThrow();
        var p = preferences.findByUsuarioId(owner).orElseGet(LembretePreferencia::new);
        p.setUsuarioId(owner);
        p.setAtivo(body.ativo());
        p.setDiario(body.diario());
        p.setSono(body.sono());
        p.setConsultas(body.consultas());
        p.setCiclo(body.ciclo());
        p.setNavegadorAtivo(body.navegadorAtivo());
        p.setEmailAtivo(body.emailAtivo());
        p.setHorario(body.horario().withSecond(0).withNano(0));
        p.setFusoHorario(body.fusoHorario());
        p.setDiasSemana(body.diasSemana().stream().distinct().sorted().map(String::valueOf).collect(Collectors.joining(",")));
        p.setAntecedenciaConsulta(body.antecedenciaConsulta());
        p.setAntecedenciaCiclo(body.antecedenciaCiclo());
        preferences.save(p);
        return new LembreteConfig(toDto(p), emailAvailable());
    }

    public List<Notice> today() {
        Long owner = current.id();
        return preferences.findByUsuarioId(owner).map(p -> due(owner, p, clock.instant())).orElse(List.of());
    }

    public void read(String key) {
        Long owner = current.id();
        users.lockById(owner).orElseThrow();
        var p = preferences.findByUsuarioId(owner).orElseThrow(() -> new IllegalArgumentException("Configure os lembretes primeiro."));
        if (due(owner, p, clock.instant()).stream().noneMatch(n -> n.chave().equals(key))) return;
        LocalDate date = clock.instant().atZone(ZoneId.of(p.getFusoHorario())).toLocalDate();
        if (deliveries.findByUsuarioIdAndDataReferenciaAndCanalAndChave(owner, date, "APP", key).isEmpty()) {
            var receipt = new LembreteEntrega();
            receipt.setUsuarioId(owner);
            receipt.setDataReferencia(date);
            receipt.setCanal("APP");
            receipt.setChave(key);
            receipt.setStatus("LIDO");
            receipt.setTentativas(1);
            receipt.setUltimaTentativa(clock.instant());
            deliveries.save(receipt);
        }
    }

    private List<Notice> due(Long owner, LembretePreferencia p, Instant instant) {
        var now = instant.atZone(ZoneId.of(p.getFusoHorario()));
        var date = now.toLocalDate();
        if (!p.isAtivo() || now.toLocalTime().isBefore(p.getHorario()) || !Arrays.asList(p.getDiasSemana().split(",")).contains(String.valueOf(now.getDayOfWeek().getValue())))
            return List.of();
        Set<String> read = deliveries.findByUsuarioIdAndDataReferenciaAndCanal(owner, date, "APP").stream().map(LembreteEntrega::getChave).collect(Collectors.toSet());
        List<Notice> result = new ArrayList<>();
        if (p.isDiario() && diary.findByUsuarioIdAndDataRegistro(owner, date).isEmpty())
            result.add(new Notice("DIARIO", "Um momento para você", "Se quiser, reserve um instante para registrar seu dia.", "pages/diario.html"));
        if (p.isSono() && sleeps.findTopByUsuarioIdAndDataRegistroOrderByIdDesc(owner, date).isEmpty())
            result.add(new Notice("SONO", "Como foi seu descanso?", "Você escolheu lembrar de registrar seu sono.", "pages/diario.html#sono"));
        if (p.isConsultas())
            for (var c : appointments.findByUsuarioIdAndStatusAndDataAgendadaGreaterThanEqualOrderByDataAgendadaAscHoraAgendadaAsc(owner, "AGENDADA", date)) {
                long days = ChronoUnit.DAYS.between(date, c.getDataAgendada());
                if (days <= p.getAntecedenciaConsulta())
                    result.add(new Notice("CONSULTA:" + c.getId(), "Um compromisso se aproxima", days == 0 ? "Há um compromisso na sua agenda hoje." : "Confira sua agenda para os próximos dias.", "pages/consultas.html"));
            }
        if (p.isCiclo()) {
            var history = cycles.findByUsuarioIdOrderByDataUltimaMenstruacaoAscIdAsc(owner);
            if (!history.isEmpty()) {
                var last = history.get(history.size() - 1);
                if (!date.isBefore(last.getDataUltimaMenstruacao().plusDays(last.getDuracaoMenstruacao() == null ? 5 : last.getDuracaoMenstruacao())))
                    projection.project(history, date).previsoes().stream().filter(f -> !f.inicioMaisTarde().isBefore(date)).findFirst().ifPresent(f -> {
                        if (!date.isBefore(f.inicioMaisCedo().minusDays(p.getAntecedenciaCiclo())))
                            result.add(new Notice("CICLO:" + last.getId(), "Seu calendário merece um olhar", "Há uma estimativa no calendário para os próximos dias. Ela não confirma um novo ciclo.", "pages/ciclo.html"));
                    });
            }
        }
        return result.stream().filter(n -> !read.contains(n.chave())).toList();
    }

    /**
     * One discreet daily e-mail, only after explicit opt-in. Serialize with account changes.
     */
    public void sendDailyEmail(Long owner) {
        if (!emailAvailable()) return;
        var user = users.lockById(owner).orElse(null);
        if (user == null) return;
        var p = preferences.findByUsuarioId(owner).orElse(null);
        if (p == null || !p.isEmailAtivo() || due(owner, p, clock.instant()).isEmpty()) return;
        var date = clock.instant().atZone(ZoneId.of(p.getFusoHorario())).toLocalDate();
        var receipt = deliveries.findByUsuarioIdAndDataReferenciaAndCanalAndChave(owner, date, "EMAIL", "RESUMO").orElseGet(LembreteEntrega::new);
        if ("ENVIADO".equals(receipt.getStatus()) || receipt.getTentativas() >= 3 || receipt.getUltimaTentativa() != null && receipt.getUltimaTentativa().plusSeconds(900).isAfter(clock.instant()))
            return;
        receipt.setUsuarioId(owner);
        receipt.setDataReferencia(date);
        receipt.setCanal("EMAIL");
        receipt.setChave("RESUMO");
        receipt.setTentativas(receipt.getTentativas() + 1);
        receipt.setUltimaTentativa(clock.instant());
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("Seu lembrete do Espelho Vital");
        message.setText("Você escolheu receber lembretes. Há um cuidado para lembrar hoje. Acesse seu espaço: " + frontend.replaceAll("/+$", "") + "/perfil-pronto.html\nVocê pode desativar estes e-mails em Minha conta > Lembretes.");
        try {
            mail.getObject().send(message);
            receipt.setStatus("ENVIADO");
        } catch (org.springframework.mail.MailException ex) {
            receipt.setStatus("FALHOU");
        }
        deliveries.save(receipt);
    }

    private Preferences toDto(LembretePreferencia p) {
        return new Preferences(p.isAtivo(), p.isDiario(), p.isSono(), p.isConsultas(), p.isCiclo(), p.isNavegadorAtivo(), p.isEmailAtivo(), p.getHorario(), p.getFusoHorario(), Arrays.stream(p.getDiasSemana().split(",")).map(Integer::valueOf).toList(), p.getAntecedenciaConsulta(), p.getAntecedenciaCiclo());
    }
}
