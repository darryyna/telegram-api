package edu.lytvyniuk.telegrambot.config.handlers;

/*
  @author darin
  @project telegram-bot
  @class TranslationCommandHandler
  @version 1.0.0
  @since 18.04.2026 - 16.26
*/

import edu.lytvyniuk.telegrambot.service.translation.TranslationParserService;
import edu.lytvyniuk.telegrambot.service.translation.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
@Component
@RequiredArgsConstructor
public class TranslationCommandHandler {

    private final TranslationService translationService;
    private final TranslationParserService translationParser;

    private static final Map<String, String> LANG_DISPLAY = Map.of(
            "en", "🇬🇧 English",
            "de", "🇩🇪 Deutsch",
            "uk", "🇺🇦 Українська"
    );

    public String handle(Long userId, String input) {
        Optional<TranslationParserService.ParseResult> parsed = translationParser.parse(input);

        if (parsed.isEmpty()) {
            return buildUsageHelp();
        }

        TranslationParserService.ParseResult result = parsed.get();
        String targetCode = translationService.resolveLang(result.targetLang());

        if (targetCode == null) {
            return String.format("""
                    Мова *%s* не підтримується.

                    Доступні мови:
                    — `en` / `англійська` / `english`
                    — `de` / `німецька` / `german`
                    — `uk` / `українська` / `ukrainian`
                    """, result.targetLang());
        }

        String translated = translationService.translate(result.text(), targetCode);

        if (translated == null) {
            return "❌ Не вдалося виконати переклад. Спробуйте пізніше.";
        }

        String langLabel = LANG_DISPLAY.getOrDefault(targetCode, targetCode.toUpperCase());

        return String.format("""
                🌐 *Переклад → %s*

                *Оригінал:*
                _%s_

                *Переклад:*
                %s
                """, langLabel, escapeMarkdown(result.text()), translated);
    }

    private String buildUsageHelp() {
        return """
                *Переклад тексту*

                Використання:
                `/translate en Привіт, як справи?`
                `/translate de Hello, how are you?`
                `/translate uk Good morning!`

                Або природньою мовою:
                `Переклади на англійську: Привіт`
                `Translate to German: Гарного дня`

                Підтримувані мови:
                — `en` — Англійська
                — `de` — Німецька
                — `uk` — Українська
                """;
    }

    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("_", "\\_").replace("*", "\\*").replace("`", "\\`");
    }
}
