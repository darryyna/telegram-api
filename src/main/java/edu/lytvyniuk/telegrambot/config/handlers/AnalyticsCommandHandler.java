package edu.lytvyniuk.telegrambot.config.handlers;

/*
  @author darin
  @project telegram-bot
  @class AnalyticsCommandHandler
  @version 1.0.0
  @since 18.04.2026 - 16.33
*/

import edu.lytvyniuk.telegrambot.service.analytics.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsCommandHandler {

    private final AnalyticsService analyticsService;

    public String handleUserStats(Long telegramId) {
        try {
            return analyticsService.buildUserStats(telegramId);
        } catch (Exception e) {
            log.error("Error building user stats for {}: {}", telegramId, e.getMessage());
            return "❌ Не вдалося отримати статистику. Спробуйте пізніше.";
        }
    }

    public String handleExport() {
        try {
            analyticsService.exportToCsv();
            analyticsService.exportToJson();
            return "✅ Звіти CSV та JSON збережено у директорії `reports/`.";
        } catch (Exception e) {
            log.error("Export failed: {}", e.getMessage());
            return "❌ Не вдалося сформувати звіт: " + e.getMessage();
        }
    }
}
