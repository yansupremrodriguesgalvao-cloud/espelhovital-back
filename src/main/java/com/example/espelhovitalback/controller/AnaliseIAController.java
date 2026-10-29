package com.example.espelhovitalback.controller;

import com.example.espelhovitalback.service.AnaliseIAService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/analise-ia")
public class AnaliseIAController {
    public AnaliseIAController(AnaliseIAService service) {
        this.service = service;
    }


    private final AnaliseIAService service;

    /**
     * Insights automáticos cruzando sono, humor e ciclo menstrual.
     */
    @GetMapping
    public Map<String, Object> analisar() {
        return service.analisar();
    }
}
