package edu.lytvyniuk.telegrambot.service;

import edu.lytvyniuk.telegrambot.DTO.CurrencyResponse;
import edu.lytvyniuk.telegrambot.DTO.NewsResponse;
import edu.lytvyniuk.telegrambot.DTO.WeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/*
  @author darin
  @project telegram-bot
  @class SmartApiService
  @version 1.0.0
  @since 18.03.2026 - 23.17
*/

@Service
@RequiredArgsConstructor
@Slf4j
public class SmartApiService {

    private final RestTemplate restTemplate;

    @Value("${smartapi.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Fetches weather data for a given city from SmartAPI.
     *
     * @param city the city name
     * @return WeatherResponse or null on error
     */
    public WeatherResponse getWeather(String city) {
        String url = baseUrl + "/weather?city={city}";
        log.info("Calling SmartAPI weather for city: {}", city);
        try {
            return restTemplate.getForObject(url, WeatherResponse.class, city);
        } catch (Exception e) {
            log.error("Error fetching weather for {}: {}", city, e.getMessage());
            return null;
        }
    }

    /**
     * Fetches currency exchange rates from SmartAPI.
     *
     * @param base the base currency (e.g., "USD")
     * @return CurrencyResponse or null on error
     */
    public CurrencyResponse getCurrency(String base) {
        String url = baseUrl + "/currency?base={base}";
        log.info("Calling SmartAPI currency for base: {}", base);
        try {
            return restTemplate.getForObject(url, CurrencyResponse.class, base);
        } catch (Exception e) {
            log.error("Error fetching currency for {}: {}", base, e.getMessage());
            return null;
        }
    }

    /**
     * Fetches news from SmartAPI for the given category.
     *
     * @param category news category (e.g., "general", "technology")
     * @return NewsResponse or null on error
     */
    public NewsResponse getNews(String category) {
        String url = baseUrl + "/news?category={category}";
        log.info("Calling SmartAPI news for category: {}", category);
        try {
            return restTemplate.getForObject(url, NewsResponse.class, category);
        } catch (Exception e) {
            log.error("Error fetching news for {}: {}", category, e.getMessage());
            return null;
        }
    }
}