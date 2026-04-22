package edu.lytvyniuk.telegrambot.config.handlers;

import edu.lytvyniuk.telegrambot.service.analytics.AnalyticsService;
import edu.lytvyniuk.telegrambot.service.analytics.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.ByteArrayInputStream;
import java.util.Map;

/*
  @author darin
  @project telegram-bot
  @class AnalyticsCommandHandler
  @version 2.0.0
  @since 22.04.2026
*/

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsCommandHandler {

    private final AnalyticsService analyticsService;
    private final ExportService exportService;

    public String handleUserStats(Long telegramId) {
        try {
            return analyticsService.buildUserStats(telegramId);
        } catch (Exception e) {
            log.error("Error building user stats for {}: {}", telegramId, e.getMessage());
            return "Не вдалося отримати статистику. Спробуйте пізніше.";
        }
    }

    public SendDocument handleExportCsv(String chatId, Long telegramId) {

        byte[] file = exportService.exportCsv(telegramId);

        InputFile inputFile = new InputFile(
                new ByteArrayInputStream(file),
                "report.csv"
        );

        SendDocument message = new SendDocument();
        message.setChatId(chatId);
        message.setDocument(inputFile);
        message.setCaption("CSV звіт за останні 7 днів");

        return message;
    }

    public SendDocument handleExportJson(String chatId, Long telegramId) {

        byte[] file = exportService.exportJson(telegramId);

        InputFile inputFile = new InputFile(
                new ByteArrayInputStream(file),
                "report.json"
        );

        SendDocument message = new SendDocument();
        message.setChatId(chatId);
        message.setDocument(inputFile);
        message.setCaption("JSON звіт за останні 7 днів");

        return message;
    }

    public String handleExportMenu() {
        return """
                *Вибір формату експорту:*

                Доступні команди:
                `/report csv` — Експорт у CSV
                `/report json` — Експорт у JSON
                `/report both` — Обидва формати

                Звіти включають дані за останні *7 днів*.
                """;
    }

    private String formatSize(String content) {
        int sizeKb = content.getBytes().length / 1024;
        return sizeKb > 0 ? sizeKb + " KB" : "< 1 KB";
    }
}