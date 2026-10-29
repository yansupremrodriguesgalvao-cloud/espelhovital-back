package com.example.espelhovitalback.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record HistoryPage<T>(List<T> registros, int pagina, int totalPaginas, long totalRegistros, boolean temProxima) {
    public static <T> HistoryPage<T> of(Page<T> page) {
        return new HistoryPage<>(page.getContent(), page.getNumber(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }
}
