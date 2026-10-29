package com.example.espelhovitalback.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Service
public class GrokService {
    public record Message(String role, String content) {
    }

    public record Request(String model, List<Message> messages, int max_tokens) {
    }

    public record Choice(Message message) {
    }

    public record Response(List<Choice> choices) {
    }

    private final RestClient client;
    private final String model;
    private final boolean configured;

    public GrokService(@Value("${xai.api.key:}") String key, @Value("${xai.api.url:https://api.groq.com/openai/v1}") String url,
                       @Value("${xai.api.model:llama-3.1-8b-instant}") String model) {
        this.model = model;
        this.configured = !key.isBlank() && !key.equals("COLOQUE_SUA_CHAVE_AQUI");
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(30));
        client = RestClient.builder().baseUrl(url).requestFactory(factory).defaultHeader("Authorization", "Bearer " + key)
                .defaultHeader("Content-Type", "application/json").build();
    }

    public String conversar(String message) {
        if (!configured)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Assistente indisponÃ­vel no momento.");
        try {
            Response response = client.post().uri("/chat/completions")
                    .body(new Request(model, List.of(new Message("user", message)), 800)).retrieve().body(Response.class);
            if (response == null || response.choices() == null || response.choices().isEmpty() || response.choices().get(0).message() == null
                    || response.choices().get(0).message().content() == null)
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "O assistente nÃ£o retornou uma resposta.");
            return response.choices().get(0).message().content();
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "O assistente estÃ¡ temporariamente indisponÃ­vel. Tente novamente.");
        }
    }
}
