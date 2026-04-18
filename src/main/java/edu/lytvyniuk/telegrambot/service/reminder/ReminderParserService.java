package edu.lytvyniuk.telegrambot.service.reminder;

/*
  @author darin
  @project telegram-bot
  @class ReminderParserService
  @version 1.0.0
  @since 18.04.2026 - 15.23
*/
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.*;

@Service
@Slf4j
public class ReminderParserService {

    public static final ZoneId KYIV_ZONE = ZoneId.of("Europe/Kyiv");

    private static final Pattern RELATIVE_PATTERN = Pattern.compile(
            "(?:через|in)\\s+(\\d+)\\s+" +
                    "(?:(хвилин|хвилини|хвилину|minute|minutes|min)" +
                    "|(годин|години|годину|hour|hours|hr)" +
                    "|(днів|дні|дня|день|day|days))",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TOMORROW_PATTERN = Pattern.compile(
            "(?:завтра|tomorrow)\\s+(?:о|at)\\s+(\\d{1,2}[:\\.]\\d{2})",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TODAY_PATTERN = Pattern.compile(
            "(?:сьогодні|today)\\s+(?:о|at)\\s+(\\d{1,2}[:\\.]\\d{2})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_ONLY_PATTERN = Pattern.compile(
            "(?:^|\\s)(?:о|at)\\s+(\\d{1,2}[:\\.]\\d{2})(?:\\s|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DATE_TIME_PATTERN = Pattern.compile(
            "(\\d{1,2}\\.\\d{1,2}(?:\\.\\d{4})?|\\d{4}-\\d{2}-\\d{2})" +
                    "\\s+(?:о|at)?\\s*(\\d{1,2}[:\\.]\\d{2})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PREFIX_NOISE = Pattern.compile(
            "(?i)^(?:нагадай(?:\\s+мені)?|remind(?:\\s+me)?)\\s*",
            Pattern.CASE_INSENSITIVE);

    public record ParseResult(LocalDateTime remindAt, String message) {}
    public Optional<ParseResult> parse(String text) {
        String cleaned = text.trim();
        Optional<ParseResult> result = tryRelative(cleaned)
                .or(() -> tryTomorrow(cleaned))
                .or(() -> tryToday(cleaned))
                .or(() -> tryDateTime(cleaned))
                .or(() -> tryTimeOnly(cleaned));

        return result.filter(r -> r.remindAt().isAfter(nowKyiv()));
    }

    public LocalDateTime nowKyiv() {
        return LocalDateTime.now(KYIV_ZONE);
    }

    private Optional<ParseResult> tryRelative(String text) {
        Matcher m = RELATIVE_PATTERN.matcher(text);
        if (!m.find()) return Optional.empty();

        int amount = Integer.parseInt(m.group(1));
        LocalDateTime base = nowKyiv();
        LocalDateTime remindAt;

        if (m.group(2) != null)      remindAt = base.plusMinutes(amount);
        else if (m.group(3) != null) remindAt = base.plusHours(amount);
        else                          remindAt = base.plusDays(amount);

        String msg = extractMessageAfterMatch(text, m);
        return Optional.of(new ParseResult(remindAt, msg));
    }

    private Optional<ParseResult> tryTomorrow(String text) {
        Matcher m = TOMORROW_PATTERN.matcher(text);
        if (!m.find()) return Optional.empty();

        LocalTime time = parseTime(m.group(1));
        if (time == null) return Optional.empty();

        LocalDateTime remindAt = nowKyiv().toLocalDate().plusDays(1).atTime(time);
        String msg = extractMessageAfterMatch(text, m);
        return Optional.of(new ParseResult(remindAt, msg));
    }

    private Optional<ParseResult> tryToday(String text) {
        Matcher m = TODAY_PATTERN.matcher(text);
        if (!m.find()) return Optional.empty();

        LocalTime time = parseTime(m.group(1));
        if (time == null) return Optional.empty();

        LocalDateTime remindAt = nowKyiv().toLocalDate().atTime(time);
        String msg = extractMessageAfterMatch(text, m);
        return Optional.of(new ParseResult(remindAt, msg));
    }

    private Optional<ParseResult> tryDateTime(String text) {
        Matcher m = DATE_TIME_PATTERN.matcher(text);
        if (!m.find()) return Optional.empty();

        String datePart = m.group(1);
        LocalTime time  = parseTime(m.group(2));
        if (time == null) return Optional.empty();

        LocalDate date = parseDate(datePart);
        if (date == null) return Optional.empty();

        LocalDateTime remindAt = date.atTime(time);
        String msg = extractMessageAfterMatch(text, m);
        return Optional.of(new ParseResult(remindAt, msg));
    }

    private Optional<ParseResult> tryTimeOnly(String text) {
        Matcher m = TIME_ONLY_PATTERN.matcher(text);
        if (!m.find()) return Optional.empty();

        LocalTime time = parseTime(m.group(1));
        if (time == null) return Optional.empty();

        LocalDateTime candidate = nowKyiv().toLocalDate().atTime(time);
        if (!candidate.isAfter(nowKyiv())) candidate = candidate.plusDays(1);

        String msg = extractMessageAfterMatch(text, m);
        return Optional.of(new ParseResult(candidate, msg));
    }

    private String extractMessageAfterMatch(String text, Matcher m) {
        String after = text.substring(m.end()).trim();
        after = after.replaceFirst("(?i)^(про|about|to|-)\\s*", "").trim();

        if (!after.isEmpty()) return capitalise(after);
        String stripped = PREFIX_NOISE.matcher(text).replaceFirst("").trim();
        stripped = stripped.replace(m.group(0).trim(), "").trim();
        stripped = stripped.replaceFirst("(?i)^(про|about|to|-)\\s*", "").trim();
        return capitalise(stripped.isEmpty() ? "Нагадування" : stripped);
    }

    private String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private LocalTime parseTime(String raw) {
        try {
            String normalised = raw.replace('.', ':');
            String[] parts = normalised.split(":");
            int h = Integer.parseInt(parts[0]);
            int min = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            if (h < 0 || h > 23 || min < 0 || min > 59) return null;
            return LocalTime.of(h, min);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDate(String raw) {
        // yyyy-MM-dd
        try { return LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE); }
        catch (DateTimeParseException ignored) {}

        // dd.MM.yyyy
        try { return LocalDate.parse(raw, DateTimeFormatter.ofPattern("dd.MM.yyyy")); }
        catch (DateTimeParseException ignored) {}

        if (raw.matches("\\d{1,2}\\.\\d{1,2}")) {
            try {
                String[] parts = raw.split("\\.");
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int year = nowKyiv().getYear();
                LocalDate candidate = LocalDate.of(year, month, day);
                if (candidate.isBefore(nowKyiv().toLocalDate())) {
                    candidate = candidate.plusYears(1);
                }
                return candidate;
            } catch (Exception ignored) {}
        }
        return null;
    }
}
