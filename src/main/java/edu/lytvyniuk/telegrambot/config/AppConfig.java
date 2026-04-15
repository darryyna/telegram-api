package edu.lytvyniuk.telegrambot.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
/*
  @author darin
  @project telegram-bot
  @class AppConfig
  @version 1.0.0
  @since 18.03.2026 - 23.12
*/

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}