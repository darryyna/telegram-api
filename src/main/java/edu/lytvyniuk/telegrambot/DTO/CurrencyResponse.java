package edu.lytvyniuk.telegrambot.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
/*
  @author darin
  @project telegram-bot
  @class CurrencyResponse
  @version 1.0.0
  @since 18.03.2026 - 23.15
*/

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyResponse {
    private String base;
    private Map<String, Double> rates;
}