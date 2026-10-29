package com.example.espelhovitalback.service;


import com.example.espelhovitalback.model.PasswordReset;
import com.example.espelhovitalback.repository.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.security.*;
import java.time.Instant;
import java.util.*;
import java.nio.charset.StandardCharsets;

@Service
public class PasswordRecoveryService {
    private final UsuarioRepository users;
    private final PasswordResetRepository tokens;
    private final PasswordEncoder encoder;
    private final ObjectProvider<JavaMailSender> mail;
    private final String frontend, from, host;

    public PasswordRecoveryService(UsuarioRepository users, PasswordResetRepository tokens, PasswordEncoder encoder,
                                   ObjectProvider<JavaMailSender> mail, @Value("${app.frontend-url:http://localhost:5500}") String frontend,
                                   @Value("${app.mail.from:noreply@espelhovital.local}") String from, @Value("${spring.mail.host:}") String host) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.mail = mail;
        this.frontend = frontend;
        this.from = from;
        this.host = host;
    }

    @Transactional
    public void request(String email) {
        if (host.isBlank() || mail.getIfAvailable() == null)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RecuperaÃ§Ã£o por e-mail indisponÃ­vel no momento.");
        users.findByEmailIgnoreCase(email.trim()).ifPresent(user -> {
            byte[] bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            PasswordReset reset = tokens.findByUsuarioId(user.getId()).orElseGet(PasswordReset::new);
            reset.setUsuarioId(user.getId());
            reset.setTokenHash(hash(token));
            reset.setExpiresAt(Instant.now().plusSeconds(1800));
            tokens.save(reset);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(user.getEmail());
            message.setSubject("Espelho Vital â€” redefiniÃ§Ã£o de senha");
            message.setText("Use este link em atÃ© 30 minutos para redefinir sua senha: " + frontend.replaceAll("/+$", "") + "/pages/redefinir-senha.html#token=" + token + "\nSe vocÃª nÃ£o solicitou, ignore esta mensagem.");
            try {
                mail.getObject().send(message);
            } catch (org.springframework.mail.MailException ex) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RecuperaÃ§Ã£o por e-mail indisponÃ­vel no momento.");
            }
        });
    }

    @Transactional
    public void reset(String token, String password) {
        PasswordReset reset = tokens.findByTokenHash(hash(token)).filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("Link invÃ¡lido ou expirado. Solicite outro link."));
        var user = users.findById(reset.getUsuarioId()).orElseThrow();
        user.setSenha(encoder.encode(password));
        user.setAuthVersion(user.getAuthVersion() + 1);
        users.save(user);
        tokens.delete(reset);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
