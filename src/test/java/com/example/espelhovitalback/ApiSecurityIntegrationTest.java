package com.example.espelhovitalback;

import com.example.espelhovitalback.model.*;
import com.example.espelhovitalback.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSecurityIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    UsuarioRepository users;
    @Autowired
    SonoRepository sleeps;
    @Autowired
    ConsultaRepository appointments;
    @Autowired
    PasswordResetRepository tokens;
    @Autowired
    PasswordEncoder encoder;

    private Usuario user() {
        var u = new Usuario("Teste", UUID.randomUUID() + "@example.com", encoder.encode("password123"));
        return users.save(u);
    }

    private MockHttpSession login(Usuario u) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/login").with(csrf()).with(request -> {
                            request.setRemoteAddr("127.1.0." + u.getId());
                            return request;
                        }).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("email", u.getEmail(), "senha", "password123"))))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    @Test
    void privateRoutesRequireAuthenticationAndWritesRequireCsrf() throws Exception {
        mvc.perform(get("/sono")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/login").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void realSessionAndCsrfEndpointWorkTogether() throws Exception {
        var u = user();
        var result = mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        var csrf = json.readTree(result.getResponse().getContentAsString());
        var session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(post("/api/login").session(session).header(csrf.get("headerName").asText(), csrf.get("token").asText())
                        .contentType("application/json").content(json.writeValueAsString(Map.of("email", u.getEmail(), "senha", "password123"))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/sessao").session(session)).andExpect(status().isOk()).andExpect(jsonPath("id").value(u.getId()));
    }

    @Test
    void dataAndDashboardNeverLeakOtherAccountsAndBodyOwnerIsIgnored() throws Exception {
        var a = user();
        var b = user();
        var session = login(a);
        var sleep = new Sono();
        sleep.setUsuarioId(b.getId());
        sleep.setHoras(9.0);
        sleep.setQualidade("Boa");
        sleeps.save(sleep);
        var appointment = new Consulta();
        appointment.setUsuarioId(b.getId());
        appointment.setTipo("EXAME");
        appointment.setTitulo("Privado");
        appointment.setDataAgendada(LocalDate.now().plusDays(1));
        appointments.save(appointment);
        mvc.perform(get("/sono").session(session).param("usuarioId", b.getId().toString())).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/dashboard").session(session)).andExpect(status().isOk()).andExpect(jsonPath("totalConsultasProximas").value(0));
        mvc.perform(get("/usuarios/" + b.getId()).session(session)).andExpect(status().isForbidden());
        mvc.perform(delete("/consultas/" + appointment.getId()).session(session).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/sono").session(session).with(csrf()).contentType("application/json")
                        .content("{\"horas\":7,\"qualidade\":\"Boa\",\"usuarioId\":" + b.getId() + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("usuarioId").value(a.getId()));
        mvc.perform(get("/analise-ia").session(session)).andExpect(status().isOk()).andExpect(jsonPath("mediaHorasSono").value(7.0));
    }

    @Test
    void cycleHistoryCanBeCorrectedAndDeletedAndNeverChangesOtherUsers() throws Exception {
        var a = user();
        var b = user();
        var session = login(a);
        String record = "{\"dataUltimaMenstruacao\":\"2026-01-01\",\"duracaoCiclo\":28,\"duracaoMenstruacao\":5,\"irregular\":true,\"duracaoMinima\":24,\"duracaoMaxima\":40}";
        for (int i = 0; i < 2; i++)
            mvc.perform(post("/ciclo/calcular").session(session).with(csrf()).contentType("application/json").content(record)).andExpect(status().isOk());
        var list = mvc.perform(get("/ciclo").session(session)).andExpect(status().isOk()).andReturn();
        var rows = json.readTree(list.getResponse().getContentAsString());
        assertThat(rows.size()).isEqualTo(1);
        long id = rows.get(0).get("id").asLong();
        mvc.perform(put("/ciclo/" + id).session(login(b)).with(csrf()).contentType("application/json").content(record)).andExpect(status().isNotFound());
        mvc.perform(put("/ciclo/" + id).session(session).with(csrf()).contentType("application/json").content(record.replace("2026-01-01", "2026-01-03"))).andExpect(status().isOk());
        mvc.perform(get("/ciclo/calendario").session(session)).andExpect(status().isOk()).andExpect(jsonPath("irregular").value(true)).andExpect(jsonPath("registros[0].inicio").value("2026-01-03"));
        mvc.perform(delete("/ciclo/" + id).session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/ciclo/calendario").session(session)).andExpect(jsonPath("registros").isEmpty()).andExpect(jsonPath("previsoes").isEmpty());
    }

    @Test
    void rejectsInvalidCycleAndSleepInputs() throws Exception {
        var session = login(user());
        mvc.perform(post("/ciclo/calcular").session(session).with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("campos.dataUltimaMenstruacao").exists());
        mvc.perform(post("/sono").session(session).with(csrf()).contentType("application/json").content("{\"horas\":99,\"qualidade\":\"Boa\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetTokenIsSingleUseAndRevokesExistingSessions() throws Exception {
        var u = user();
        var session = login(u);
        String token = UUID.randomUUID().toString();
        var reset = new PasswordReset();
        reset.setUsuarioId(u.getId());
        reset.setExpiresAt(Instant.now().plusSeconds(600));
        reset.setTokenHash(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))));
        tokens.save(reset);
        var body = json.writeValueAsString(Map.of("token", token, "senha", "newpassword123"));
        mvc.perform(post("/api/senha/redefinir").with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(post("/api/senha/redefinir").with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        mvc.perform(get("/sono").session(session)).andExpect(status().isUnauthorized());
        assertThat(encoder.matches("newpassword123", users.findById(u.getId()).orElseThrow().getSenha())).isTrue();
    }

    @Test
    void logoutInvalidatesTheSession() throws Exception {
        var session = login(user());
        mvc.perform(post("/api/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void supportsFractionalHoursAndDoesNotBindClientIdsIntoExistingRecords() throws Exception {
        var a = user();
        var b = user();
        var record = new Sono();
        record.setUsuarioId(b.getId());
        record.setHoras(9.0);
        record.setQualidade("Boa");
        record = sleeps.save(record);
        mvc.perform(post("/sono").session(login(a)).with(csrf()).contentType("application/json")
                        .content("{\"id\":" + record.getId() + ",\"usuarioId\":" + b.getId() + ",\"horas\":7.5,\"qualidade\":\"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("horas").value(7.5)).andExpect(jsonPath("qualidade").value("Boa"))
                .andExpect(jsonPath("usuarioId").value(a.getId()));
        assertThat(sleeps.findById(record.getId()).orElseThrow().getHoras()).isEqualTo(9.0);
    }

    @Test
    void facialRegistrationRequiresPasswordAndCannotReplaceSomeoneElsesFace() throws Exception {
        var a = user();
        var b = user();
        var session = login(a);
        var face = Collections.nCopies(128, 0.1);
        mvc.perform(post("/usuarios/face/cadastrar").session(session).with(csrf()).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("usuarioId", b.getId(), "senha", "incorrect", "descritor", face))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/usuarios/face/cadastrar").session(session).with(csrf()).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("usuarioId", b.getId(), "senha", "password123", "descritor", face))))
                .andExpect(status().isOk());
        assertThat(users.findById(a.getId()).orElseThrow().getFaceDescriptor()).isNotBlank();
        assertThat(users.findById(b.getId()).orElseThrow().getFaceDescriptor()).isNull();
        mvc.perform(post("/usuarios/face/login").with(csrf()).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("descritor", face))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvertedIrregularRangesAndFutureStarts() throws Exception {
        var session = login(user());
        mvc.perform(post("/ciclo/calcular").session(session).with(csrf()).contentType("application/json")
                        .content("{\"dataUltimaMenstruacao\":\"2026-01-01\",\"duracaoCiclo\":28,\"duracaoMenstruacao\":5,\"irregular\":true,\"duracaoMinima\":40,\"duracaoMaxima\":24}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/ciclo/calcular").session(session).with(csrf()).contentType("application/json")
                        .content("{\"dataUltimaMenstruacao\":\"" + LocalDate.now().plusDays(1) + "\",\"duracaoCiclo\":28,\"duracaoMenstruacao\":5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paginatedHistoriesRemainScopedAndRejectUnboundedPageSizes() throws Exception {
        var a = user();
        var b = user();
        var session = login(a);
        for (int i = 0; i < 3; i++) {
            var r = new Sono();
            r.setUsuarioId(a.getId());
            r.setHoras((double) (5 + i));
            r.setQualidade("Boa");
            sleeps.save(r);
        }
        var other = new Sono();
        other.setUsuarioId(b.getId());
        other.setHoras(10.0);
        other.setQualidade("Boa");
        sleeps.save(other);
        mvc.perform(get("/sono/historico").session(session).param("tamanho", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("registros.length()").value(2)).andExpect(jsonPath("totalRegistros").value(3))
                .andExpect(jsonPath("temProxima").value(true));
        mvc.perform(get("/sono/historico").session(session).param("tamanho", "100000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsExpiredRecoveryTokensAndReportsUnconfiguredEmailDelivery() throws Exception {
        var u = user();
        var reset = new PasswordReset();
        String token = UUID.randomUUID().toString();
        reset.setUsuarioId(u.getId());
        reset.setExpiresAt(Instant.now().minusSeconds(1));
        reset.setTokenHash(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))));
        tokens.save(reset);
        mvc.perform(post("/api/senha/redefinir").with(csrf()).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("token", token, "senha", "newpassword123"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/senha/recuperar").with(csrf()).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("email", u.getEmail()))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void corsAllowsOnlyConfiguredOrigins() throws Exception {
        mvc.perform(options("/sono").header("Origin", "http://localhost:5500").header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/sono").header("Origin", "http://untrusted.example").header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
