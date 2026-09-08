package com.example.espelhovitalback.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class AnaliseIAService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String responder(String pergunta) {

        String apiKey = System.getenv("XAI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            return "A chave da Assistente Vital não foi configurada.";
        }

        String url =
                "https://api.x.ai/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.setBearerAuth(apiKey);

        String mensagemSistema =
                "Você é a Assistente Vital do Espelho Vital. "
                        + "Seja acolhedora, delicada, clara e objetiva. "
                        + "Responda sempre em português do Brasil. "
                        + "Você pode ajudar com informações gerais sobre "
                        + "bem-estar, ciclo menstrual, autocuidado, sono "
                        + "e emoções. "
                        + "Não faça diagnósticos médicos. "
                        + "Quando uma situação exigir avaliação profissional, "
                        + "oriente a usuária a procurar um profissional de saúde.";

        Map<String, Object> mensagemSistemaMap =
                Map.of(
                        "role", "system",
                        "content", mensagemSistema
                );

        Map<String, Object> mensagemUsuario =
                Map.of(
                        "role", "user",
                        "content", pergunta
                );

        Map<String, Object> corpo =
                Map.of(
                        "model", "grok-4.6",
                        "messages", List.of(
                                mensagemSistemaMap,
                                mensagemUsuario
                        ),
                        "stream", false
                );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(
                        corpo,
                        headers
                );

        try {

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(
                            url,
                            request,
                            Map.class
                    );

            Map resposta = response.getBody();

            if (resposta == null) {
                return "Não consegui receber uma resposta da Assistente Vital.";
            }

            List choices =
                    (List) resposta.get("choices");

            if (choices == null || choices.isEmpty()) {
                return "A Assistente Vital não retornou uma resposta.";
            }

            Map primeiraChoice =
                    (Map) choices.get(0);

            Map message =
                    (Map) primeiraChoice.get("message");

            if (message == null) {
                return "Não consegui interpretar a resposta da Assistente Vital.";
            }

            Object content =
                    message.get("content");

            if (content == null) {
                return "A Assistente Vital não retornou texto.";
            }

            return content.toString();

        } catch (Exception e) {

            e.printStackTrace();

            return "Não foi possível conectar com a Assistente Vital.";
        }
    }
}