package com.example.espelhovitalback;

import com.example.espelhovitalback.model.Usuario;
import com.example.espelhovitalback.repository.UsuarioRepository;
import com.example.espelhovitalback.repository.CicloMenstrualRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CycleQuickStartIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    UsuarioRepository users;
    @Autowired
    CicloMenstrualRepository cycles;
    @Autowired
    PasswordEncoder encoder;

    private Usuario user() {
        return users.save(new Usuario("Teste ciclo", UUID.randomUUID() + "@example.com", encoder.encode("password123")));
    }

    private MockHttpSession login(Usuario user) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/login").with(csrf()).with(r -> {
            r.setRemoteAddr("quick-" + user.getId());
            return r;
        }).contentType("application/json").content(json.writeValueAsString(Map.of("email", user.getEmail(), "senha", "password123")))).andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private void first(MockHttpSession session, LocalDate date, boolean irregular) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dataUltimaMenstruacao", date);
        body.put("duracaoCiclo", 28);
        body.put("duracaoMenstruacao", 6);
        body.put("irregular", irregular);
        if (irregular) {
            body.put("duracaoMinima", 24);
            body.put("duracaoMaxima", 40);
        }
        mvc.perform(post("/ciclo/calcular").session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isOk());
    }

    @Test
    void actualEndCanShortenExtendAndCorrectAnIrregularPeriod() throws Exception {
        var owner = user();
        var session = login(owner);
        var start = LocalDate.now().minusDays(30);
        first(session, start, true);
        long id = cycles.findByUsuarioIdOrderByDataUltimaMenstruacaoAscIdAsc(owner.getId()).get(0).getId();
        for (int duration : List.of(3, 10, 10, 1)) {
            mvc.perform(post("/ciclo/" + id + "/fim").session(session).with(csrf()).contentType("application/json")
                    .content(date(start.plusDays(duration - 1))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("registros[0].duracaoMenstruacao").value(duration))
                    .andExpect(jsonPath("registros[0].fimMenstruacao").value(start.plusDays(duration - 1).toString()))
                    .andExpect(jsonPath("registros[0].duracaoMinima").value(24))
                    .andExpect(jsonPath("irregular").value(true));
        }
    }

    @Test
    void endRequiresOwnershipValidDateAndCannotOverlapNextStart() throws Exception {
        var owner = user();
        var session = login(owner);
        var start = LocalDate.now().minusDays(60);
        first(session, start, false);
        long id = cycles.findByUsuarioIdOrderByDataUltimaMenstruacaoAscIdAsc(owner.getId()).get(0).getId();
        first(session, start.plusDays(28), false);
        String endpoint = "/ciclo/" + id + "/fim";
        mvc.perform(post(endpoint).session(login(user())).with(csrf()).contentType("application/json").content(date(start))).andExpect(status().isNotFound());
        mvc.perform(post(endpoint).session(session).contentType("application/json").content(date(start))).andExpect(status().isForbidden());
        for (var invalid : List.of(start.minusDays(1), LocalDate.now().plusDays(1)))
            mvc.perform(post(endpoint).session(session).with(csrf()).contentType("application/json").content(date(invalid))).andExpect(status().isBadRequest());
        mvc.perform(post(endpoint).session(session).with(csrf()).contentType("application/json").content(date(start.plusDays(28)))).andExpect(status().isConflict());
        mvc.perform(post(endpoint).session(session).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
    }

    private String date(LocalDate date) throws Exception {
        return json.writeValueAsString(Map.of("data", date));
    }

    @Test
    void aDateAloneReusesSavedSettingsAndReanchorsForecasts() throws Exception {
        var user = user();
        var session = login(user);
        var start = LocalDate.now().minusDays(60);
        first(session, start, false);
        mvc.perform(post("/ciclo/inicio").session(session).with(csrf()).contentType("application/json").content(date(start.plusDays(35))))
                .andExpect(status().isOk()).andExpect(jsonPath("criado").value(true)).andExpect(jsonPath("calendario.registros.length()").value(2))
                .andExpect(jsonPath("calendario.registros[1].duracaoMenstruacao").value(6))
                .andExpect(jsonPath("calendario.duracaoEstimada").value(35))
                .andExpect(jsonPath("calendario.previsoes[0].inicio").value(start.plusDays(70).toString()));
        assertThat(cycles.countByUsuarioId(user.getId())).isEqualTo(2);
    }

    @Test
    void preservesIrregularRangeAndDoesNotInventFertility() throws Exception {
        var user = user();
        var session = login(user);
        var start = LocalDate.now().minusDays(60);
        first(session, start, true);
        mvc.perform(post("/ciclo/inicio").session(session).with(csrf()).contentType("application/json").content(date(start.plusDays(30))))
                .andExpect(status().isOk()).andExpect(jsonPath("calendario.irregular").value(true))
                .andExpect(jsonPath("calendario.registros[1].irregular").value(true))
                .andExpect(jsonPath("calendario.registros[1].duracaoMinima").value(24))
                .andExpect(jsonPath("calendario.registros[1].duracaoMaxima").value(40))
                .andExpect(jsonPath("calendario.previsoes[0].ovulacao").isEmpty());
    }

    @Test
    void repeatedConfirmationsAreIdempotentAndDoNotOfferUndoForExistingStarts() throws Exception {
        var user = user();
        var session = login(user);
        var start = LocalDate.now().minusDays(60);
        first(session, start, false);
        var next = start.plusDays(28);
        var created = mvc.perform(post("/ciclo/inicio").session(session).with(csrf()).contentType("application/json").content(date(next))).andExpect(status().isOk()).andReturn();
        long id = json.readTree(created.getResponse().getContentAsString()).get("registroId").asLong();
        mvc.perform(post("/ciclo/inicio").session(session).with(csrf()).contentType("application/json").content(date(next)))
                .andExpect(status().isOk()).andExpect(jsonPath("criado").value(false)).andExpect(jsonPath("registroId").value(id));
        assertThat(cycles.countByUsuarioId(user.getId())).isEqualTo(2);
    }

    @Test
    void rejectsOverlapOnEitherSideOfAnExistingPeriod() throws Exception {
        var user = user();
        var session = login(user);
        var start = LocalDate.now().minusDays(60);
        first(session, start, false);
        for (var selected : List.of(start.plusDays(2), start.minusDays(2)))
            mvc.perform(post("/ciclo/inicio").session(session).with(csrf()).contentType("application/json").content(date(selected))).andExpect(status().isConflict());
        assertThat(cycles.countByUsuarioId(user.getId())).isEqualTo(1);
    }

    @Test
    void firstSetupIsRequiredForTheAuthenticatedAccount() throws Exception {
        var owner = user();
        first(login(owner), LocalDate.now().minusDays(60), true);
        var another = user();
        var session = login(another);
        mvc.perform(post("/ciclo/inicio").session(session).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("data", LocalDate.now(), "usuarioId", owner.getId()))))
                .andExpect(status().isConflict());
        assertThat(cycles.countByUsuarioId(owner.getId())).isEqualTo(1);
        assertThat(cycles.countByUsuarioId(another.getId())).isZero();
    }

    @Test
    void validatesDateAndRequiresSessionAndCsrf() throws Exception {
        var session = login(user());
        mvc.perform(post("/ciclo/inicio").with(csrf()).contentType("application/json").content(date(LocalDate.now()))).andExpect(status().isUnauthorized());
        mvc.perform(post("/ciclo/inicio").session(session).contentType("application/json").content(date(LocalDate.now()))).andExpect(status().isForbidden());
        for (String body : List.of("{}", date(LocalDate.now().plusDays(1)), date(LocalDate.of(1899, 1, 1))))
            mvc.perform(post("/ciclo/inicio").session(session).with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
    }

    @Test
    void undoThroughScopedDeletionRestoresPreviousEstimate() throws Exception {
        var user = user();
        var session = login(user);
        var start = LocalDate.now().minusDays(60);
        first(session, start, false);
        var result = mvc.perform(post("/ciclo/inicio").session(session).with(csrf()).contentType("application/json").content(date(start.plusDays(35)))).andExpect(status().isOk()).andReturn();
        long id = json.readTree(result.getResponse().getContentAsString()).get("registroId").asLong();
        mvc.perform(delete("/ciclo/" + id).session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/ciclo/calendario").session(session)).andExpect(jsonPath("duracaoEstimada").value(28)).andExpect(jsonPath("registros.length()").value(1));
    }
}
