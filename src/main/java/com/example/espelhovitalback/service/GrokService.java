package com.example.espelhovitalback.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class GrokService {

    private final RestClient restClient;
    private final boolean apiKeyConfigurada;

    public GrokService(@Value("${xai.api.key:}") String apiKey) {

        this.apiKeyConfigurada = apiKey != null
                && !apiKey.isBlank()
                && !apiKey.equals("COLOQUE_SUA_CHAVE_AQUI");

        if (!apiKeyConfigurada) {
            System.out.println(
                    "[GrokService] Aviso: a propriedade xai.api.key não foi configurada em " +
                    "application.properties. O assistente de IA ficará indisponível até que ela seja definida."
            );
        }

        this.restClient = RestClient.builder()
                .baseUrl("https://api.x.ai/v1")
                .defaultHeader("Authorization", "Bearer " + (apiKeyConfigurada ? apiKey : ""))
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String conversar(String mensagem) {

        if (!apiKeyConfigurada) {
            throw new IllegalStateException(
                    "A propriedade xai.api.key não foi configurada em application.properties. " +
                    "Preencha o valor de xai.api.key para usar o assistente de IA."
            );
        }

        Map<String, Object> body = Map.of(
                "model", "grok-4.5",
                "messages", new Object[]{
                        Map.of(
                                "role", "user",
                                "content", mensagem
                        )
                }
        );

        Map resposta = restClient.post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(Map.class);

        var choices = (java.util.List<Map>) resposta.get("choices");

        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("O Grok não retornou uma resposta.");
        }

        Map message = (Map) choices.get(0).get("message");

        return String.valueOf(message.get("content"));
    }
}
