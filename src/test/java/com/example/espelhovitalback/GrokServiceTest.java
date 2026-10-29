package com.example.espelhovitalback;

import com.example.espelhovitalback.service.GrokService;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GrokServiceTest {
    @Test
    void sendsTypedRequestAndReadsProviderResponse() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> request = new AtomicReference<>();
        server.createContext("/chat/completions", exchange -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Olá\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            var service = new GrokService("test-key", "http://127.0.0.1:" + server.getAddress().getPort(), "test-model");
            assertThat(service.conversar("Teste")).isEqualTo("Olá");
            var body = new ObjectMapper().readTree(request.get());
            assertThat(body.get("model").asText()).isEqualTo("test-model");
            assertThat(body.get("max_tokens").asInt()).isEqualTo(800);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void translatesProviderErrorsWithoutExposingResponseBody() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            byte[] bytes = "private upstream details".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            var service = new GrokService("test-key", "http://127.0.0.1:" + server.getAddress().getPort(), "test-model");
            var error = assertThrows(ResponseStatusException.class, () -> service.conversar("Teste"));
            assertThat(error.getStatusCode().value()).isEqualTo(502);
            assertThat(error.getReason()).doesNotContain("private");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void unconfiguredProviderFailsBeforeSendingAnyRequest() {
        var service = new GrokService("", "http://127.0.0.1:1", "test-model");
        assertThat(assertThrows(ResponseStatusException.class, () -> service.conversar("Teste")).getStatusCode().value()).isEqualTo(503);
    }
}
