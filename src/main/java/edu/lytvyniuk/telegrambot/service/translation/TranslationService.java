package edu.lytvyniuk.telegrambot.service.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/*
  @author darin
  @project telegram-bot
  @class TranslationService
  @version 4.0.0
  @since 18.04.2026
*/
@Slf4j
@Service
public class TranslationService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String GOOGLE_URL =
            "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=%s&dt=t&q=%s";

    private static final Map<String, String> LANG_ALIASES = Map.ofEntries(
            Map.entry("uk",         "uk"),
            Map.entry("ua",         "uk"),
            Map.entry("укр",        "uk"),
            Map.entry("українська", "uk"),
            Map.entry("ukrainian",  "uk"),
            Map.entry("en",         "en"),
            Map.entry("англ",       "en"),
            Map.entry("англійська", "en"),
            Map.entry("english",    "en"),
            Map.entry("de",         "de"),
            Map.entry("нім",        "de"),
            Map.entry("німецька",   "de"),
            Map.entry("german",     "de"),
            Map.entry("deutsch",    "de")
    );

    public TranslationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public String translate(String text, String targetLang) {
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = String.format(GOOGLE_URL, targetLang, encoded);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Google Translate error: status={}, body={}",
                        response.statusCode(), response.body());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            StringBuilder result = new StringBuilder();
            for (JsonNode sentence : root.get(0)) {
                JsonNode part = sentence.get(0);
                if (part != null && !part.isNull()) {
                    result.append(part.asText());
                }
            }

            String translated = result.toString().trim();
            log.info("Translated auto→{}: '{}'", targetLang, truncate(translated, 60));
            return translated.isEmpty() ? null : translated;

        } catch (Exception e) {
            log.error("Translation error: {}", e.getMessage());
            return null;
        }
    }

    public String resolveLang(String raw) {
        if (raw == null) return null;
        return LANG_ALIASES.get(raw.trim().toLowerCase());
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}