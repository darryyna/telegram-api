package edu.lytvyniuk.telegrambot.DTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
  @author darin
  @project telegram-bot
  @class WeatherResponse
  @version 1.0.0
  @since 18.03.2026 - 23.14
*/

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherResponse {
    private String city;
    private Double temperature;
    private String description;
}