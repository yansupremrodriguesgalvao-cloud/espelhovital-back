package com.example.espelhovitalback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
public class EspelhovitalBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(EspelhovitalBackApplication.class, args);
    }

}
