package edu.lytvyniuk.telegrambot.service;

import edu.lytvyniuk.telegrambot.DTO.CurrencyResponse;
import edu.lytvyniuk.telegrambot.DTO.NewsResponse;
import edu.lytvyniuk.telegrambot.DTO.WeatherResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/*
  @author darin
  @project telegram-bot
  @class MessageFormatterService
  @version 1.0.0
  @since 18.03.2026 - 23.18
*/
@Service
public class MessageFormatterService {

    private static final List<String> KEY_CURRENCIES = List.of(
            "EUR", "UAH", "GBP", "PLN", "CZK", "CHF", "JPY", "CAD"
    );

    public String formatWeather(WeatherResponse weather) {
        if (weather == null) {
            return "Could not retrieve weather data. Please check the city name and try again.";
        }
        return String.format("""
                *Weather in %s*
                
                Temperature: *%.1f C*
                Condition: %s
                """,
                weather.getCity(),
                weather.getTemperature(),
                weather.getDescription()
        );
    }

    public String formatCurrency(CurrencyResponse currency) {
        if (currency == null) {
            return "Could not retrieve exchange rates. Please try again later.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("*Exchange rates relative to %s*\n\n", currency.getBase()));

        Map<String, Double> rates = currency.getRates();
        if (rates != null) {
            for (String code : KEY_CURRENCIES) {
                if (rates.containsKey(code)) {
                    sb.append(String.format("- *%s*: %.4f\n", code, rates.get(code)));
                }
            }
        }
        sb.append("\n_To check another currency, type: /currency EUR_");
        return sb.toString();
    }

    public String formatNews(NewsResponse news) {
        if (news == null || news.getArticles() == null || news.getArticles().isEmpty()) {
            return "No news available at the moment. Please try again later.";
        }

        StringBuilder sb = new StringBuilder("*Latest news:*\n\n");
        List<NewsResponse.Article> articles = news.getArticles();

        int count = Math.min(5, articles.size());
        for (int i = 0; i < count; i++) {
            NewsResponse.Article article = articles.get(i);
            if (article.getTitle() != null) {
                sb.append(String.format("%d. [%s](%s)\n",
                        i + 1,
                        escapeMarkdown(article.getTitle()),
                        article.getUrl() != null ? article.getUrl() : "https://news.google.com"
                ));
                if (article.getDescription() != null) {
                    String desc = article.getDescription();
                    if (desc.length() > 100) desc = desc.substring(0, 100) + "...";
                    sb.append("   _").append(escapeMarkdown(desc)).append("_\n\n");
                } else {
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("-", "\\-")
                .replace("!", "\\!")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("+", "\\+")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("~", "\\~")
                .replace("`", "\\`");
    }
}