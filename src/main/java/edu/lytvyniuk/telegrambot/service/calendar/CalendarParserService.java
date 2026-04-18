package edu.lytvyniuk.telegrambot.service.calendar;

/*
  @author darin
  @project telegram-bot
  @class CalendarParserService
  @version 1.0.1
  @since 18.04.2026 - 15.29
*/
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.*;

@Service
@Slf4j
public class CalendarParserService {

    public static final ZoneId KYIV_ZONE = ZoneId.of("Europe/Kyiv");
    private static final Map<String, DayOfWeek> WEEKDAYS_UK = Map.ofEntries(
            Map.entry("понеділок", DayOfWeek.MONDAY),
            Map.entry("вівторок",  DayOfWeek.TUESDAY),
            Map.entry("середу",    DayOfWeek.WEDNESDAY),
            Map.entry("середа",    DayOfWeek.WEDNESDAY),
            Map.entry("четвер",    DayOfWeek.THURSDAY),
            Map.entry("п'ятницю",  DayOfWeek.FRIDAY),
            Map.entry("п'ятниця",  DayOfWeek.FRIDAY),
            Map.entry("суботу",    DayOfWeek.SATURDAY),
            Map.entry("субота",    DayOfWeek.SATURDAY),
            Map.entry("неділю",    DayOfWeek.SUNDAY),
            Map.entry("неділя",    DayOfWeek.SUNDAY)
    );

    private static final Map<String, DayOfWeek> WEEKDAYS_EN = Map.of(
            "monday",    DayOfWeek.MONDAY,
            "tuesday",   DayOfWeek.TUESDAY,
            "wednesday", DayOfWeek.WEDNESDAY,
            "thursday",  DayOfWeek.THURSDAY,
            "friday",    DayOfWeek.FRIDAY,
            "saturday",  DayOfWeek.SATURDAY,
            "sunday",    DayOfWeek.SUNDAY
    );

    private static final Pattern DATE_TIME_PATTERN = Pattern.compile(
            "(\\d{1,2}\\.\\d{1,2}(?:\\.\\d{4})?|\\d{4}-\\d{2}-\\d{2})" +
                    "\\s+(?:о|at)?\\s*(\\d{1,2}[:\\.]\\d{2})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern WEEKDAY_TIME_PATTERN = Pattern.compile(
            "(понеділок|вівторок|середу|середа|четвер|п'ятницю|п'ятниця|суботу|субота|неділю|неділя" +
                    "|monday|tuesday|wednesday|thursday|friday|saturday|sunday)" +
                    "\\s+(?:о|at)\\s*(\\d{1,2}[:\\.]\\d{2})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TOMORROW_PATTERN = Pattern.compile(
            "(?:завтра|tomorrow)\\s+(?:о|at)\\s*(\\d{1,2}[:\\.]\\d{2})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PREFIX_NOISE = Pattern.compile(
            "(?i)^(?:створи|додай|нова|event|create|add|calendar)\\s*" +
                    "(?:подію|event|подія)?\\s*(?:на|on)?\\s*",
            Pattern.CASE_INSENSITIVE);
    public record ParseResult(LocalDateTime eventStart, String title) {}

    public Optional<ParseResult> parse(String text) {
        String cleaned = text.trim();

        return tryDateTime(cleaned)
                .or(() -> tryWeekday(cleaned))
                .or(() -> tryTomorrow(cleaned));
    }
    private Optional<ParseResult> tryDateTime(String text) {
        Matcher m = DATE_TIME_PATTERN.matcher(text);
        if (!m.find()) return Optional.empty();

        LocalDate date = parseDate(m.group(1));
        LocalTime time = parseTime(m.group(2));
        if (date == null || time == null) return Optional.empty();

        String title = extractTitle(text, m);
        return Optional.of(new ParseResult(date.atTime(time), title));
    }

    private Optional<ParseResult> tryWeekday(String text) {
        Matcher m = WEEKDAY_TIME_PATTERN.matcher(text);
        if (!m.find()) return Optional.empty();

        String dayName = m.group(1).toLowerCase();
        DayOfWeek dow = WEEKDAYS_UK.getOrDefault(dayName, WEEKDAYS_EN.get(dayName));
        if (dow == null) return Optional.empty();

        LocalTime time = parseTime(m.group(2));
        if (time == null) return Optional.empty();

        LocalDate today = LocalDate.now(KYIV_ZONE);
        LocalDate eventDate = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(dow));
        if (eventDate.equals(today) && time.isBefore(LocalTime.now(KYIV_ZONE))) {
            eventDate = today.with(java.time.temporal.TemporalAdjusters.next(dow));
        }

        String title = extractTitle(text, m);
        return Optional.of(new ParseResult(eventDate.atTime(time), title));
    }

    private Optional<ParseResult> tryTomorrow(String text) {
        Matcher m = TOMORROW_PATTERN.matcher(text);
        if (!m.find()) return Optional.empty();

        LocalTime time = parseTime(m.group(1));
        if (time == null) return Optional.empty();

        LocalDateTime dt = LocalDate.now(KYIV_ZONE).plusDays(1).atTime(time);
        String title = extractTitle(text, m);
        return Optional.of(new ParseResult(dt, title));
    }

    private String extractTitle(String text, Matcher m) {
        String after = text.substring(m.end()).trim();
        after = after.replaceFirst("(?i)^[-–—]\\s*", "").trim();
        if (!after.isEmpty()) return capitalise(after);

        String stripped = PREFIX_NOISE.matcher(text).replaceFirst("").trim();
        stripped = stripped.replace(m.group(0).trim(), "").trim();
        return capitalise(stripped.isEmpty() ? "Нова подія" : stripped);
    }

    private String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private LocalTime parseTime(String raw) {
        try {
            String n = raw.replace('.', ':');
            String[] parts = n.split(":");
            int h = Integer.parseInt(parts[0]);
            int min = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            if (h < 0 || h > 23 || min < 0 || min > 59) return null;
            return LocalTime.of(h, min);
        } catch (Exception e) { return null; }
    }

    private LocalDate parseDate(String raw) {
        try { return LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE); }
        catch (DateTimeParseException ignored) {}
        try { return LocalDate.parse(raw, DateTimeFormatter.ofPattern("dd.MM.yyyy")); }
        catch (DateTimeParseException ignored) {}
        if (raw.matches("\\d{1,2}\\.\\d{1,2}")) {
            try {
                String[] p = raw.split("\\.");
                int day   = Integer.parseInt(p[0]);
                int month = Integer.parseInt(p[1]);
                int year  = LocalDate.now(KYIV_ZONE).getYear();
                LocalDate candidate = LocalDate.of(year, month, day);
                if (candidate.isBefore(LocalDate.now(KYIV_ZONE))) candidate = candidate.plusYears(1);
                return candidate;
            } catch (Exception ignored) {}
        }
        return null;
    }
}