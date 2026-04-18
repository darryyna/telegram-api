package edu.lytvyniuk.telegrambot.config.handlers;

/*
  @author darin
  @project telegram-bot
  @class ReminderCommandHandler
  @version 1.0.0
  @since 18.04.2026 - 15.30
*/
import edu.lytvyniuk.telegrambot.entity.reminder.Reminder;
import edu.lytvyniuk.telegrambot.service.reminder.ReminderParserService;
import edu.lytvyniuk.telegrambot.service.reminder.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReminderCommandHandler {

    private final ReminderService reminderService;
    private final ReminderParserService reminderParser;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public String handleCreate(Long telegramId, String chatId, String text) {
        if (text.isBlank())
            return """
                    Вкажіть текст нагадування. Приклади:
                    `/reminder завтра о 9:00 про зустріч`
                    `/reminder через 2 години зателефонувати`
                    `/reminder 25.04 о 14:30 Лікар`
                    """;

        Optional<ReminderParserService.ParseResult> parsed = reminderParser.parse(text);
        if (parsed.isEmpty()) {
            if (text.toLowerCase().contains("о ") || text.toLowerCase().contains("at ")) {
                return "Не вдалося розпізнати дату/час, або вказаний час вже минув.\n\n" +
                        "Приклади правильного формату:\n" +
                        "`/reminder завтра о 9:00 зустріч`\n" +
                        "`/reminder через 30 хвилин кава`\n" +
                        "`/reminder 25.04 о 10:00 Лікар`";
            }
            return "Не вдалося визначити час нагадування.\n\n" +
                    "Вкажіть час явно, наприклад:\n" +
                    "`/reminder завтра о 9:00 зустріч`";
        }

        ReminderParserService.ParseResult result = parsed.get();
        Reminder reminder = reminderService.create(
                telegramId, chatId, result.message(), result.remindAt());

        return String.format("""
                Нагадування створено!

                📝 *%s*
                🕐 %s

                _ID: #%d — для видалення: /deletereminder %d_
                """,
                reminder.getMessage(),
                reminder.getRemindAt().format(FMT),
                reminder.getId(),
                reminder.getId());
    }

    public String handleList(Long telegramId) {
        List<Reminder> reminders = reminderService.listActive(telegramId);
        return reminderService.formatReminderList(reminders);
    }

    public String handleDelete(Long telegramId, String idStr) {
        if (idStr.isBlank()) return "Вкажіть ID: `/deletereminder 5`";
        try {
            Long id = Long.parseLong(idStr.trim());
            boolean deleted = reminderService.delete(id, telegramId);
            return deleted
                    ? String.format("Нагадування *#%d* видалено.", id)
                    : "Нагадування не знайдено або не належить вам.";
        } catch (NumberFormatException e) {
            return "Невірний формат ID. Приклад: `/deletereminder 5`";
        }
    }
}
