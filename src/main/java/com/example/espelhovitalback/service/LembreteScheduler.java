package com.example.espelhovitalback.service;

import com.example.espelhovitalback.repository.LembretePreferenciaRepository;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.*;
import org.slf4j.*;

@Component
@ConditionalOnProperty(name = "app.reminders.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class LembreteScheduler {
    private static final Logger log = LoggerFactory.getLogger(LembreteScheduler.class);
    private final LembretePreferenciaRepository preferences;
    private final LembreteService service;

    public LembreteScheduler(LembretePreferenciaRepository preferences, LembreteService service) {
        this.preferences = preferences;
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${app.reminders.poll-ms:60000}", initialDelay = 60000)
    public void send() {
        if (!service.emailAvailable()) return;
        int page = 0;
        boolean next;
        do {
            var batch = preferences.findByAtivoTrueAndEmailAtivoTrue(PageRequest.of(page++, 100, Sort.by("id")));
            next = batch.hasNext();
            for (var p : batch)
                try {
                    service.sendDailyEmail(p.getUsuarioId());
                } catch (RuntimeException ex) {
                    log.warn("Não foi possível processar um lembrete: {}", ex.getClass().getSimpleName());
                }
        } while (next);
    }
}
