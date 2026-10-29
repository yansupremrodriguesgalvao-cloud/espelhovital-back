package com.example.espelhovitalback.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

/**
 * The authenticated account's saved settings supply all other cycle fields.
 */
public record CicloInicioRequest(@NotNull @PastOrPresent LocalDate data) {
}
