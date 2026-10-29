package com.example.espelhovitalback.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository repository) throws Exception {
        http.cors(Customizer.withDefaults())
                .securityContext(c -> c.securityContextRepository(repository))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/csrf", "/api/login", "/usuarios/cadastrar", "/usuarios/face/login",
                                "/api/senha/recuperar", "/api/senha/redefinir", "/error").permitAll()
                        .anyRequest().authenticated())
                .requestCache(c -> c.disable())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> json(res, 401, "Autenticação necessária."))
                        .accessDeniedHandler((req, res, ex) -> json(res, 403, "Acesso negado ou sessão expirada. Atualize a página.")))
                .logout(l -> l.logoutUrl("/api/logout").invalidateHttpSession(true).deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)));
        return http.build();
    }

    private static void json(jakarta.servlet.http.HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"mensagem\":\"" + message + "\"}");
    }
}
