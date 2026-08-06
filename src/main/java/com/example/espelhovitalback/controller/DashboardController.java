package com.example.espelhovitalback.controller;

import com.example.espelhovitalback.dto.DashboardDTO;
import com.example.espelhovitalback.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin("*")
public class DashboardController {

    @Autowired
    private DashboardService service;

    /** Retorna todos os indicadores consolidados de saúde para a tela inicial. */
    @GetMapping
    public DashboardDTO montar() {
        return service.montar();
    }
}
