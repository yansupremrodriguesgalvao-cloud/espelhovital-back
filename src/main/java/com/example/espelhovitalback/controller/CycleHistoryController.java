package com.example.espelhovitalback.controller;

import com.example.espelhovitalback.service.CycleHistoryService;
import org.springframework.web.bind.annotation.*;

@RestController
public class CycleHistoryController {
    private final CycleHistoryService service;

    public CycleHistoryController(CycleHistoryService service) {
        this.service = service;
    }

    @GetMapping("/ciclo/variacoes")
    public CycleHistoryService.History history() {
        return service.history();
    }
}
