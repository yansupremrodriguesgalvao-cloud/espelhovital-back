package com.example.espelhovitalback.controller;

import com.example.espelhovitalback.service.AnaliseIAService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ia")
@CrossOrigin(origins = "*")
public class AnaliseIAController {

    private final AnaliseIAService analiseIAService;

    public AnaliseIAController(AnaliseIAService analiseIAService) {
        this.analiseIAService = analiseIAService;
    }

    @PostMapping("/perguntar")
    public String perguntar(@RequestBody String pergunta) {
        return analiseIAService.responder(pergunta);
    }
}