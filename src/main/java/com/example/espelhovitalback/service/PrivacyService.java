package com.example.espelhovitalback.service;

import com.example.espelhovitalback.config.CurrentUser;
import com.example.espelhovitalback.model.Usuario;
import com.example.espelhovitalback.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.*;

@Service
@Transactional
public class PrivacyService {
    private final CurrentUser current;
    private final UsuarioRepository users;
    private final UsuarioService profiles;
    private final PasswordEncoder encoder;
    private final DiarioRepository diary;
    private final SonoRepository sleeps;
    private final HumorRepository moods;
    private final CicloMenstrualRepository cycles;
    private final ConsultaRepository appointments;
    private final PasswordResetRepository tokens;
    private final LembretePreferenciaRepository preferences;
    private final LembreteEntregaRepository deliveries;
    private final Clock clock;

    public PrivacyService(CurrentUser current, UsuarioRepository users, UsuarioService profiles, PasswordEncoder encoder,
                          DiarioRepository diary, SonoRepository sleeps, HumorRepository moods, CicloMenstrualRepository cycles, ConsultaRepository appointments,
                          PasswordResetRepository tokens, LembretePreferenciaRepository preferences, LembreteEntregaRepository deliveries, Clock clock) {
        this.current = current;
        this.users = users;
        this.profiles = profiles;
        this.encoder = encoder;
        this.diary = diary;
        this.sleeps = sleeps;
        this.moods = moods;
        this.cycles = cycles;
        this.appointments = appointments;
        this.tokens = tokens;
        this.preferences = preferences;
        this.deliveries = deliveries;
        this.clock = clock;
    }

    public Map<String, Object> summary() {
        Long id = current.id();
        var u = users.findById(id).orElseThrow();
        return Map.of("biometriaCadastrada", u.getFaceDescriptor() != null && !u.getFaceDescriptor().isBlank(), "registros",
                Map.of("diario", diary.countByUsuarioId(id), "sono", sleeps.countByUsuarioId(id), "humor", moods.countByUsuarioId(id), "ciclos", cycles.countByUsuarioId(id), "consultas", appointments.countByUsuarioId(id)));
    }

    public Map<String, Object> export(String password, boolean includeBiometric) {
        var u = authenticate(password);
        Long id = u.getId();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("versao", 1);
        out.put("exportadoEm", clock.instant());
        out.put("perfil", profiles.buscarPerfil(id));
        out.put("diario", diary.findByUsuarioIdOrderByDataRegistroAsc(id));
        out.put("sono", sleeps.findByUsuarioIdOrderByDataRegistroAscIdAsc(id));
        out.put("humor", moods.findByUsuarioIdOrderByDataRegistroAscIdAsc(id));
        out.put("ciclos", cycles.findByUsuarioIdOrderByDataUltimaMenstruacaoAscIdAsc(id));
        out.put("consultas", appointments.findByUsuarioIdOrderByDataAgendadaAscHoraAgendadaAscIdAsc(id));
        out.put("preferenciasLembretes", preferences.findByUsuarioId(id).orElse(null));
        out.put("historicoLembretes", deliveries.findByUsuarioIdOrderByDataReferenciaAscIdAsc(id));
        Map<String, Object> face = new LinkedHashMap<>();
        face.put("cadastrada", u.getFaceDescriptor() != null && !u.getFaceDescriptor().isBlank());
        if (includeBiometric) face.put("descritor", u.getFaceDescriptor());
        out.put("biometria", face);
        return out;
    }

    public void clearHealth(String password, String confirmation) {
        requireConfirmation(confirmation, "APAGAR MEUS REGISTROS");
        var u = authenticate(password);
        deleteHealth(u.getId());
        preferences.findByUsuarioId(u.getId()).ifPresent(p -> {
            p.setAtivo(false);
            p.setEmailAtivo(false);
            p.setNavegadorAtivo(false);
            preferences.save(p);
        });
    }

    public void deleteAccount(String password, String confirmation) {
        requireConfirmation(confirmation, "EXCLUIR MINHA CONTA");
        var u = authenticate(password);
        deleteHealth(u.getId());
        tokens.deleteByUsuarioId(u.getId());
        preferences.deleteByUsuarioId(u.getId());
        users.delete(u);
        users.flush();
    }

    public void deleteBiometric(String password) {
        var u = authenticate(password);
        u.setFaceDescriptor(null);
        users.save(u);
    }

    private Usuario authenticate(String password) {
        var u = users.lockById(current.id()).orElseThrow();
        if (!encoder.matches(password, u.getSenha()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Senha de confirmação inválida.");
        return u;
    }

    private void deleteHealth(Long id) {
        diary.deleteByUsuarioId(id);
        sleeps.deleteByUsuarioId(id);
        moods.deleteByUsuarioId(id);
        cycles.deleteByUsuarioId(id);
        appointments.deleteByUsuarioId(id);
        deliveries.deleteByUsuarioId(id);
    }

    private void requireConfirmation(String value, String expected) {
        if (!expected.equals(value))
            throw new IllegalArgumentException("Digite a frase de confirmação exatamente como indicada.");
    }
}
