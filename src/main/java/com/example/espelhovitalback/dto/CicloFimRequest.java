package com.example.espelhovitalback.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record CicloFimRequest(@NotNull @PastOrPresent LocalDate data) {}
