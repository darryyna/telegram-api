package edu.lytvyniuk.telegrambot.config.handlers;

/*
  @author darin
  @project telegram-bot
  @class CalendarCommandHandler
  @version 1.0.0
  @since 18.04.2026 - 15.31
*/

import edu.lytvyniuk.telegrambot.entity.calendarEvent.CalendarEvent;
import edu.lytvyniuk.telegrambot.service.calendar.CalendarParserService;
import edu.lytvyniuk.telegrambot.service.calendar.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CalendarCommandHandler {

    private final CalendarService calendarService;
    private final CalendarParserService calendarParser;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    public String handleCreate(Long telegramId, String text) {
        if (text.isBlank())
            return """
                    Вкажіть деталі події. Приклади:
                    `/addevent понеділок о 14:00 Мітинг з командою`
                    `/addevent завтра о 10:00 Лікар`
                    `/addevent 25.04 о 09:30 Презентація`
                    """;

        Optional<CalendarParserService.ParseResult> parsed = calendarParser.parse(text);
        if (parsed.isEmpty()) {
            return "Не вдалося визначити дату та час події.\n\n" +
                    "Приклади:\n" +
                    "`/addevent понеділок о 14:00 Нарада`\n" +
                    "`/addevent завтра о 10:00 Лікар`\n" +
                    "`/addevent 25.04 о 09:30 Презентація`";
        }

        CalendarParserService.ParseResult result = parsed.get();
        CalendarEvent event = calendarService.createEvent(
                telegramId, result.title(), result.eventStart(), null);

        String gcalNote = event.getGoogleEventId() != null
                ? "\n_✔ Також синхронізовано з Google Calendar_"
                : "";

        return String.format("""
                ✅ Подію додано до календаря!

                📌 *%s*
                📅 %s%s

                _ID: #%d — для видалення: /deleteevent %d_
                """,
                event.getTitle(),
                event.getEventStart().format(FMT),
                gcalNote,
                event.getId(),
                event.getId());
    }

    public String handleUpcoming(Long telegramId) {
        List<CalendarEvent> events = calendarService.getWeekEvents(telegramId);
        return calendarService.formatEventList(events, "Найближчі події (7 днів)");
    }

    public String handleToday(Long telegramId) {
        List<CalendarEvent> events = calendarService.getTodayEvents(telegramId);
        return calendarService.formatEventList(events, "Сьогоднішні події");
    }

    public String handleDelete(Long telegramId, String idStr) {
        if (idStr.isBlank()) return "Вкажіть ID: `/deleteevent 3`";
        try {
            Long id = Long.parseLong(idStr.trim());
            boolean deleted = calendarService.deleteEvent(id, telegramId);
            return deleted
                    ? String.format("✅ Подію *#%d* видалено.", id)
                    : "❌ Подію не знайдено або вона не належить вам.";
        } catch (NumberFormatException e) {
            return "⚠️ Невірний формат ID. Приклад: `/deleteevent 3`";
        }
    }

    public String handleAll(Long telegramId) {
        List<CalendarEvent> events = calendarService.getUpcoming(telegramId);
        return calendarService.formatEventList(events, "Всі майбутні події");
    }
}