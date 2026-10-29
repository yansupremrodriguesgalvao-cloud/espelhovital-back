package com.example.espelhovitalback.controller;

import com.example.espelhovitalback.service.ReportService;
import com.example.espelhovitalback.dto.ReportData;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

import java.time.LocalDate;
import java.util.List;
import java.io.IOException;

@RestController
@RequestMapping("/relatorios")
public class ReportController {
    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/resumo")
    public ReportData preview(@RequestParam LocalDate inicio, @RequestParam LocalDate fim,
                              @RequestParam(defaultValue = "DIARIO,SONO,HUMOR,CICLO,CONSULTAS") List<String> secoes) {
        return service.data(inicio, fim, secoes);
    }

    @GetMapping("/resumo.pdf")
    public ResponseEntity<byte[]> pdf(@RequestParam LocalDate inicio, @RequestParam LocalDate fim,
                                      @RequestParam(defaultValue = "DIARIO,SONO,HUMOR,CICLO,CONSULTAS") List<String> secoes) throws IOException {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=espelho-vital-" + inicio + "-" + fim + ".pdf")
                .body(service.pdf(service.data(inicio, fim, secoes)));
    }
}
