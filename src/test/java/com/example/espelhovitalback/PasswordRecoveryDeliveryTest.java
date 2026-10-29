package com.example.espelhovitalback;

import com.example.espelhovitalback.model.Usuario;
import com.example.espelhovitalback.repository.*;
import com.example.espelhovitalback.service.PasswordRecoveryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class PasswordRecoveryDeliveryTest {
    @Test
    @SuppressWarnings("unchecked")
    void sendsExpiringLinkAndStoresOnlyItsHash() {
        var users = mock(UsuarioRepository.class);
        var tokens = mock(PasswordResetRepository.class);
        var mail = mock(JavaMailSender.class);
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mail);
        when(provider.getObject()).thenReturn(mail);
        var user = new Usuario("Teste", "test@example.com", "hash");
        user.setId(1L);
        when(users.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
        when(tokens.findByUsuarioId(1L)).thenReturn(Optional.empty());
        var service = new PasswordRecoveryService(users, tokens, new BCryptPasswordEncoder(), provider, "https://app.example.com", "noreply@example.com", "smtp.example.com");
        service.request("test@example.com");
        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(message.capture());
        assertThat(message.getValue().getText()).contains("https://app.example.com/pages/redefinir-senha.html#token=");
        var reset = ArgumentCaptor.forClass(com.example.espelhovitalback.model.PasswordReset.class);
        verify(tokens).save(reset.capture());
        assertThat(reset.getValue().getTokenHash()).hasSize(64);
        assertThat(message.getValue().getText()).doesNotContain(reset.getValue().getTokenHash());
        assertThat(reset.getValue().getExpiresAt()).isAfter(Instant.now()).isBefore(Instant.now().plusSeconds(1900));
    }
}
