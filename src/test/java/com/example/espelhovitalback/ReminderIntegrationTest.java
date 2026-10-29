package com.example.espelhovitalback;

import com.example.espelhovitalback.model.*;
import com.example.espelhovitalback.repository.*;
import com.example.espelhovitalback.service.LembreteService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {"spring.mail.host=smtp.test", "app.reminders.scheduler-enabled=false"})
class ReminderIntegrationTest {
    @MockitoBean
    Clock clock;
    @MockitoBean
    JavaMailSender mail;
    @Autowired
    LembreteService service;
    @Autowired
    UsuarioRepository users;
    @Autowired
    LembretePreferenciaRepository preferences;
    @Autowired
    LembreteEntregaRepository deliveries;
    @Autowired
    DiarioRepository diary;
    @Autowired
    SonoRepository sleeps;
    @Autowired
    ConsultaRepository appointments;
    private final Instant time = Instant.parse("2026-10-29T23:00:00Z");

    @BeforeEach
    void time() {
        when(clock.instant()).thenReturn(time);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    private Usuario user() {
        var user = users.save(new Usuario("Teste", UUID.randomUUID() + "@example.com", "not-used"));
        var auth = new UsernamePasswordAuthenticationToken(user.getId().toString(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return user;
    }

    private LembretePreferencia pref(Usuario u) {
        var p = new LembretePreferencia();
        p.setUsuarioId(u.getId());
        p.setAtivo(true);
        p.setDiario(true);
        p.setSono(true);
        p.setConsultas(true);
        p.setHorario(LocalTime.of(20, 0));
        return preferences.save(p);
    }

    @Test
    void respectsOptInHourWeekdaysAndTimezone() {
        var u = user();
        var p = pref(u);
        assertThat(service.today()).hasSize(2);
        p.setAtivo(false);
        preferences.save(p);
        assertThat(service.today()).isEmpty();
        p.setAtivo(true);
        p.setHorario(LocalTime.of(21, 0));
        preferences.save(p);
        assertThat(service.today()).isEmpty();
        p.setHorario(LocalTime.of(20, 0));
        p.setDiasSemana("7");
        preferences.save(p);
        assertThat(service.today()).isEmpty();
        p.setDiasSemana("4");
        p.setFusoHorario("Pacific/Honolulu");
        preferences.save(p);
        assertThat(service.today()).isEmpty();
    }

    @Test
    void completedRecordsSuppressTheirRemindersAndDismissalsAreScoped() {
        var u = user();
        var p = pref(u);
        var day = LocalDate.of(2026, 10, 29);
        var entry = new Diario();
        entry.setUsuarioId(u.getId());
        entry.setDataRegistro(day);
        entry.setFluxo(Diario.Fluxo.SEM_FLUXO);
        entry.setAtualizadoEm(time);
        diary.save(entry);
        assertThat(service.today()).extracting(LembreteService.Notice::chave).containsExactly("SONO");
        service.read("SONO");
        assertThat(service.today()).isEmpty();
        var another = user();
        pref(another);
        assertThat(service.today()).hasSize(2);
        assertThat(deliveries.findByUsuarioIdAndDataReferenciaAndCanal(another.getId(), day, "APP")).isEmpty();
    }

    @Test
    void appointmentLeadTimeUsesOnlyOwnerAndScheduledEvents() {
        var u = user();
        var p = pref(u);
        p.setDiario(false);
        p.setSono(false);
        p.setAntecedenciaConsulta(2);
        preferences.save(p);
        var a = new Consulta();
        a.setUsuarioId(u.getId());
        a.setTipo("CONSULTA");
        a.setTitulo("Teste");
        a.setDataAgendada(LocalDate.of(2026, 10, 31));
        appointments.save(a);
        var other = new Consulta();
        other.setUsuarioId(users.save(new Usuario("Outro", UUID.randomUUID() + "@example.com", "x")).getId());
        other.setTipo("EXAME");
        other.setTitulo("Não mostrar");
        other.setDataAgendada(LocalDate.of(2026, 10, 29));
        appointments.save(other);
        assertThat(service.today()).extracting(LembreteService.Notice::chave).containsExactly("CONSULTA:" + a.getId());
        a.setStatus("CANCELADA");
        appointments.save(a);
        assertThat(service.today()).isEmpty();
    }

    @Test
    void emailIsOptInAndSentAtMostOncePerDay() {
        var u = user();
        var p = pref(u);
        service.sendDailyEmail(u.getId());
        verifyNoInteractions(mail);
        p.setEmailAtivo(true);
        preferences.save(p);
        service.sendDailyEmail(u.getId());
        service.sendDailyEmail(u.getId());
        verify(mail, times(1)).send(any(SimpleMailMessage.class));
        var receipt = deliveries.findByUsuarioIdAndDataReferenciaAndCanalAndChave(u.getId(), LocalDate.of(2026, 10, 29), "EMAIL", "RESUMO").orElseThrow();
        assertThat(receipt.getStatus()).isEqualTo("ENVIADO");
    }

    @Test
    void failedEmailHasCooldownAndBoundedRetries() {
        var u = user();
        var p = pref(u);
        p.setEmailAtivo(true);
        preferences.save(p);
        doThrow(new MailSendException("SMTP indisponível")).when(mail).send(any(SimpleMailMessage.class));
        service.sendDailyEmail(u.getId());
        service.sendDailyEmail(u.getId());
        verify(mail, times(1)).send(any(SimpleMailMessage.class));
        for (int i = 1; i <= 4; i++) {
            when(clock.instant()).thenReturn(time.plusSeconds(i * 901));
            service.sendDailyEmail(u.getId());
        }
        verify(mail, times(3)).send(any(SimpleMailMessage.class));
        assertThat(deliveries.findByUsuarioIdAndDataReferenciaAndCanalAndChave(u.getId(), LocalDate.of(2026, 10, 29), "EMAIL", "RESUMO").orElseThrow().getTentativas()).isEqualTo(3);
    }

    @Test
    void emailContainsNoHealthDetails() {
        var u = user();
        var p = pref(u);
        p.setEmailAtivo(true);
        preferences.save(p);
        service.sendDailyEmail(u.getId());
        var capture = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(capture.capture());
        assertThat(capture.getValue().getTo()).containsExactly(u.getEmail());
        assertThat(capture.getValue().getText()).doesNotContain("menstruação", "dor", "humor").contains("desativar");
    }
}
