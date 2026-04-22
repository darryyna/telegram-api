package edu.lytvyniuk.telegrambot.service.translation;

/*
  @author darin
  @project telegram-bot
  @class TranslationParserService
  @version 1.0.0
  @since 18.04.2026 - 16.25
*/
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TranslationParserService {

    private static final Pattern COMMAND_PATTERN = Pattern.compile(
            "^/translate\\s+(\\S+)\\s+(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern FREE_TEXT_PATTERN = Pattern.compile(
            "(?:переклади(?:\\s+на)?|translate(?:\\s+to)?)\\s+" +
                    "([а-яА-ЯіїєґІЇЄҐa-zA-Z]+)\\s*[:\\-–—]?\\s*(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public record ParseResult(String targetLang, String text) {}

    public Optional<ParseResult> parse(String input) {
        String trimmed = input.trim();
        Matcher cmd = COMMAND_PATTERN.matcher(trimmed);
        if (cmd.matches()) {
            return Optional.of(new ParseResult(cmd.group(1).trim(), cmd.group(2).trim()));
        }
        Matcher free = FREE_TEXT_PATTERN.matcher(trimmed);
        if (free.find()) {
            return Optional.of(new ParseResult(free.group(1).trim(), free.group(2).trim()));
        }

        return Optional.empty();
    }
}
