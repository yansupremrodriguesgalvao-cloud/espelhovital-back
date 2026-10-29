package com.example.espelhovitalback;

import com.example.espelhovitalback.model.*;
import com.example.espelhovitalback.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DiaryPrivacyReportIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    UsuarioRepository users;
    @Autowired
    PasswordEncoder encoder;
    @Autowired
    DiarioRepository diary;
    @Autowired
    SonoRepository sleeps;
    @Autowired
    HumorRepository moods;
    @Autowired
    CicloMenstrualRepository cycles;
    @Autowired
    ConsultaRepository appointments;
    @Autowired
    PasswordResetRepository tokens;
    @Autowired
    LembretePreferenciaRepository preferences;
    @Autowired
    LembreteEntregaRepository deliveries;

    private Usuario user() {
        return users.save(new Usuario("Júlia Teste", UUID.randomUUID() + "@example.com", encoder.encode("password123")));
    }

    private MockHttpSession login(Usuario user) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/login").with(csrf()).with(r -> {
            r.setRemoteAddr("test-" + user.getId());
            return r;
        }).contentType("application/json").content(json.writeValueAsString(Map.of("email", user.getEmail(), "senha", "password123")))).andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private String entry(String note) throws Exception {
        return json.writeValueAsString(Map.of("fluxo", "LEVE", "intensidadeDor", 3, "sintomas", List.of("COLICA", "INCHACO"), "humor", "Tranquila", "horasSono", 7.5, "qualidadeSono", "Boa", "observacoes", note));
    }

    private void save(MockHttpSession session, LocalDate day, String note) throws Exception {
        mvc.perform(put("/diario/" + day).session(session).with(csrf()).contentType("application/json").content(entry(note))).andExpect(status().isOk());
    }

    private String confirmation(String password, String phrase) throws Exception {
        return json.writeValueAsString(Map.of("senha", password, "confirmacao", phrase));
    }

    @Test
    void journalCreatesAndEditsHistoricalSignalsWithoutDuplicates() throws Exception {
        var user = user();
        var session = login(user);
        var date = LocalDate.now().minusDays(3);
        save(session, date, "Observação inicial");
        save(session, date, "Atualização com atenção");
        assertThat(diary.countByUsuarioId(user.getId())).isEqualTo(1);
        assertThat(sleeps.countByUsuarioId(user.getId())).isEqualTo(1);
        assertThat(moods.countByUsuarioId(user.getId())).isEqualTo(1);
        assertThat(sleeps.findTopByUsuarioIdAndDataRegistroOrderByIdDesc(user.getId(), date)).isPresent();
        mvc.perform(get("/diario/" + date).session(session)).andExpect(jsonPath("observacoes").value("Atualização com atenção")).andExpect(jsonPath("horasSono").value(7.5));
        mvc.perform(get("/diario").session(session).param("inicio", date.toString()).param("fim", date.toString())).andExpect(jsonPath("totalRegistros").value(1));
    }

    @Test
    void standaloneScreensAndDiaryShareTheSameDailyRecords() throws Exception {
        var user = user();
        var session = login(user);
        save(session, LocalDate.now(), "Meu dia");
        mvc.perform(post("/sono").session(session).with(csrf()).contentType("application/json").content("{\"horas\":8.2,\"qualidade\":\"Boa\"}")).andExpect(status().isOk());
        mvc.perform(post("/humor").session(session).with(csrf()).contentType("application/json").content("{\"estado\":\"Feliz\"}")).andExpect(status().isOk());
        mvc.perform(get("/diario/" + LocalDate.now()).session(session)).andExpect(jsonPath("horasSono").value(8.2)).andExpect(jsonPath("humor").value("Feliz"));
        assertThat(sleeps.countByUsuarioId(user.getId())).isEqualTo(1);
        assertThat(moods.countByUsuarioId(user.getId())).isEqualTo(1);
    }

    @Test
    void diaryIsScopedAndDeletionRemovesOnlyLinkedOwnerRecords() throws Exception {
        var a = user();
        var b = user();
        var one = login(a);
        var two = login(b);
        var day = LocalDate.now();
        save(one, day, "Privado A");
        save(two, day, "Privado B");
        mvc.perform(get("/diario/" + day).session(one).param("usuarioId", b.getId().toString())).andExpect(jsonPath("observacoes").value("Privado A"));
        mvc.perform(delete("/diario/" + day).session(one).with(csrf())).andExpect(status().isNoContent());
        assertThat(diary.countByUsuarioId(a.getId())).isZero();
        assertThat(sleeps.countByUsuarioId(a.getId())).isZero();
        assertThat(moods.countByUsuarioId(a.getId())).isZero();
        assertThat(diary.countByUsuarioId(b.getId())).isEqualTo(1);
        assertThat(sleeps.countByUsuarioId(b.getId())).isEqualTo(1);
    }

    @Test
    void validatesJournalDatesEnumsAndEmptyBodies() throws Exception {
        var session = login(user());
        mvc.perform(put("/diario/" + LocalDate.now().plusDays(1)).session(session).with(csrf()).contentType("application/json").content(entry("Teste"))).andExpect(status().isBadRequest());
        mvc.perform(put("/diario/" + LocalDate.now()).session(session).with(csrf()).contentType("application/json").content("{\"fluxo\":\"NAO_INFORMADO\",\"sintomas\":[]}")).andExpect(status().isBadRequest());
        mvc.perform(put("/diario/" + LocalDate.now()).session(session).with(csrf()).contentType("application/json").content(entry("Teste").replace("COLICA", "INVALIDO"))).andExpect(status().isBadRequest());
        mvc.perform(get("/diario/não-é-data").session(session)).andExpect(status().isBadRequest());
    }

    @Test
    void reportIsARealPdfWithAccentsAndOnlySelectedSectionsAndOwnerData() throws Exception {
        var a = user();
        var b = user();
        var session = login(a);
        var day = LocalDate.now();
        save(session, day, "Cólica e sensibilidade. Atenção à rotina.");
        save(login(b), day, "Segredo de outra conta");
        var result = mvc.perform(get("/relatorios/resumo.pdf").session(session).param("inicio", day.toString()).param("fim", day.toString()).param("secoes", "DIARIO"))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf")).andExpect(header().string("Cache-Control", "no-store")).andReturn();
        try (var document = Loader.loadPDF(result.getResponse().getContentAsByteArray())) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Júlia", "Cólica e sensibilidade", "Atenção à rotina").doesNotContain("Segredo de outra conta", "Inícios menstruais registrados", "Sono:", "Humor:");
        }
        mvc.perform(get("/relatorios/resumo").session(session).param("inicio", day.toString()).param("fim", day.toString()).param("secoes", "SONO"))
                .andExpect(jsonPath("diario").isEmpty()).andExpect(jsonPath("sono.length()").value(1));
    }

    @Test
    void reportWrapsLongNotesAndPaginates() throws Exception {
        var session = login(user());
        var day = LocalDate.now();
        for (int i = 0; i < 4; i++) save(session, day.minusDays(i), "Observação longa " + "á".repeat(1800));
        var result = mvc.perform(get("/relatorios/resumo.pdf").session(session).param("inicio", day.minusDays(4).toString()).param("fim", day.toString()).param("secoes", "DIARIO")).andExpect(status().isOk()).andReturn();
        try (var doc = Loader.loadPDF(result.getResponse().getContentAsByteArray())) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
            assertThat(new PDFTextStripper().getText(doc)).contains("Observação longa");
        }
        mvc.perform(get("/relatorios/resumo").session(session).param("inicio", day.minusDays(500).toString()).param("fim", day.toString())).andExpect(status().isBadRequest());
    }

    @Test
    void exportRequiresPasswordAndDoesNotExposeSecretsOrOtherAccounts() throws Exception {
        var a = user();
        var b = user();
        a.setFaceDescriptor("biometria-teste");
        users.save(a);
        var session = login(a);
        save(session, LocalDate.now(), "Meu registro");
        save(login(b), LocalDate.now(), "Outro segredo");
        mvc.perform(post("/conta/exportar").session(session).with(csrf()).contentType("application/json").content("{\"senha\":\"incorreta\"}")).andExpect(status().isForbidden());
        var result = mvc.perform(post("/conta/exportar").session(session).with(csrf()).contentType("application/json").content("{\"senha\":\"password123\"}")).andExpect(status().isOk()).andReturn();
        String exported = result.getResponse().getContentAsString();
        assertThat(exported).contains("Meu registro").doesNotContain("Outro segredo", a.getSenha(), "biometria-teste", "tokenHash");
        mvc.perform(post("/conta/exportar").session(session).with(csrf()).contentType("application/json").content("{\"senha\":\"password123\",\"incluirBiometria\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("biometria.descritor").value("biometria-teste"));
    }

    @Test
    void clearingHealthPreservesAccountAndBiometricAndPausesReminders() throws Exception {
        var a = user();
        a.setFaceDescriptor("face");
        users.save(a);
        var session = login(a);
        save(session, LocalDate.now(), "Diário");
        var p = new LembretePreferencia();
        p.setUsuarioId(a.getId());
        p.setAtivo(true);
        preferences.save(p);
        mvc.perform(delete("/conta/dados").session(session).with(csrf()).contentType("application/json").content(confirmation("password123", "frase errada"))).andExpect(status().isBadRequest());
        mvc.perform(delete("/conta/dados").session(session).with(csrf()).contentType("application/json").content(confirmation("password123", "APAGAR MEUS REGISTROS"))).andExpect(status().isNoContent());
        assertThat(diary.countByUsuarioId(a.getId())).isZero();
        assertThat(sleeps.countByUsuarioId(a.getId())).isZero();
        assertThat(moods.countByUsuarioId(a.getId())).isZero();
        assertThat(users.findById(a.getId()).orElseThrow().getFaceDescriptor()).isEqualTo("face");
        assertThat(preferences.findByUsuarioId(a.getId()).orElseThrow().isAtivo()).isFalse();
        mvc.perform(get("/api/sessao").session(session)).andExpect(status().isOk());
    }

    @Test
    void deletingAccountCleansEveryOwnedTableAndInvalidatesSession() throws Exception {
        var a = user();
        var b = user();
        var session = login(a);
        save(session, LocalDate.now(), "Apagar");
        save(login(b), LocalDate.now(), "Manter");
        var reset = new PasswordReset();
        reset.setUsuarioId(a.getId());
        reset.setTokenHash(UUID.randomUUID().toString());
        reset.setExpiresAt(Instant.now().plusSeconds(60));
        tokens.save(reset);
        var p = new LembretePreferencia();
        p.setUsuarioId(a.getId());
        preferences.save(p);
        mvc.perform(delete("/conta").session(session).with(csrf()).contentType("application/json").content(confirmation("password123", "EXCLUIR MINHA CONTA"))).andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        assertThat(users.findById(a.getId())).isEmpty();
        assertThat(tokens.findByUsuarioId(a.getId())).isEmpty();
        assertThat(preferences.findByUsuarioId(a.getId())).isEmpty();
        assertThat(diary.countByUsuarioId(a.getId())).isZero();
        assertThat(sleeps.countByUsuarioId(a.getId())).isZero();
        assertThat(moods.countByUsuarioId(a.getId())).isZero();
        assertThat(diary.countByUsuarioId(b.getId())).isEqualTo(1);
    }

    @Test
    void biometricRemovalDoesNotRemoveOtherData() throws Exception {
        var a = user();
        a.setFaceDescriptor("face");
        users.save(a);
        var session = login(a);
        save(session, LocalDate.now(), "Manter diário");
        mvc.perform(delete("/conta/biometria").session(session).with(csrf()).contentType("application/json").content("{\"senha\":\"password123\"}")).andExpect(status().isNoContent());
        assertThat(users.findById(a.getId()).orElseThrow().getFaceDescriptor()).isNull();
        assertThat(diary.countByUsuarioId(a.getId())).isEqualTo(1);
    }

    @Test
    void newEndpointsRequireAuthenticationAndRejectUnavailableEmail() throws Exception {
        for (String path : List.of("/diario/" + LocalDate.now(), "/conta/privacidade", "/lembretes/config", "/ciclo/variacoes", "/relatorios/resumo"))
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        var session = login(user());
        mvc.perform(get("/lembretes/config").session(session)).andExpect(jsonPath("preferencias.ativo").value(false)).andExpect(jsonPath("emailDisponivel").value(false));
        mvc.perform(put("/lembretes/config").session(session).with(csrf()).contentType("application/json").content("{\"ativo\":true,\"diario\":true,\"emailAtivo\":true,\"horario\":\"20:00\",\"fusoHorario\":\"America/Sao_Paulo\",\"diasSemana\":[1,2,3,4,5,6,7]}"))
                .andExpect(status().isBadRequest());
    }
}
