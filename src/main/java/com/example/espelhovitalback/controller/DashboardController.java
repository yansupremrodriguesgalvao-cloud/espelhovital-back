package com.example.espelhovitalback.controller;

import com.example.espelhovitalback.dto.DashboardDTO;
import com.example.espelhovitalback.service.DashboardService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    public DashboardController(DashboardService service) {
        this.service = service;
    }


    private final DashboardService service;

    /**
     * Retorna todos os indicadores consolidados de saúde para a tela inicial.
     */
    @GetMapping
    public DashboardDTO montar(@org.springframework.web.bind.annotation.RequestParam(required = false) Long usuarioId) {
        return service.montar(usuarioId);
    }
}
