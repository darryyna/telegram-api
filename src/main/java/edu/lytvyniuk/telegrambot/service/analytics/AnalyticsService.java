package edu.lytvyniuk.telegrambot.service.analytics;

/*
  @author darin
  @project telegram-bot
  @class AnalyticsService
  @version 1.0.0
  @since 18.04.2026 - 16.32
*/
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.lytvyniuk.telegrambot.entity.logs.RequestLog;
import edu.lytvyniuk.telegrambot.entity.logs.RequestLogRepository;
import edu.lytvyniuk.telegrambot.entity.logs.RequestType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final RequestLogRepository logRepository;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Transactional
    public void log(Long telegramId, String chatId, String rawText,
                    RequestType type, boolean success, String errorMessage) {
        RequestLog entry = RequestLog.builder()
                .telegramId(telegramId)
                .chatId(chatId)
                .rawText(rawText)
                .requestType(type)
                .success(success)
                .errorMessage(errorMessage)
                .build();
        logRepository.save(entry);
    }

    @Transactional
    public void logSuccess(Long telegramId, String chatId, String rawText, RequestType type) {
        log(telegramId, chatId, rawText, type, true, null);
    }

    @Transactional
    public void logError(Long telegramId, String chatId, String rawText,
                         RequestType type, String errorMessage) {
        log(telegramId, chatId, rawText, type, false, errorMessage);
    }

    public String buildUserStats(Long telegramId) {
        LocalDateTime dayAgo  = LocalDateTime.now().minusDays(1);
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        long today = logRepository.countByTelegramIdAndCreatedAtAfter(telegramId, dayAgo);
        long week  = logRepository.countByTelegramIdAndCreatedAtAfter(telegramId, weekAgo);
        long total = logRepository.countByTelegramId(telegramId);

        List<Object[]> topCmds = logRepository.topCommandsByUser(telegramId, weekAgo);

        StringBuilder sb = new StringBuilder("*📊 Ваша статистика:*\n\n");
        sb.append(String.format("Сьогодні: *%d* запитів\n", today));
        sb.append(String.format("За тиждень: *%d* запитів\n", week));
        sb.append(String.format("Всього: *%d* запитів\n\n", total));

        if (!topCmds.isEmpty()) {
            sb.append("*Топ команди (7 днів):*\n");
            int rank = 1;
            for (Object[] row : topCmds) {
                RequestType type = (RequestType) row[0];
                Long count = (Long) row[1];
                sb.append(String.format("%d. %s — *%d*\n", rank++, formatType(type), count));
                if (rank > 5) break;
            }
        }
        return sb.toString();
    }

    public String buildGlobalStats() {
        LocalDateTime dayAgo  = LocalDateTime.now().minusDays(1);
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        long today  = logRepository.countByCreatedAtAfter(dayAgo);
        long week   = logRepository.countByCreatedAtAfter(weekAgo);
        long errors = logRepository.countBySuccessFalseAndCreatedAtAfter(weekAgo);

        List<Object[]> top = logRepository.topCommandsSince(weekAgo);

        StringBuilder sb = new StringBuilder("*📈 Глобальна статистика бота:*\n\n");
        sb.append(String.format("За 24 год: *%d* запитів\n", today));
        sb.append(String.format("За тиждень: *%d* запитів\n", week));
        sb.append(String.format("Помилки (7 днів): *%d*\n\n", errors));

        if (!top.isEmpty()) {
            sb.append("*Топ команди:*\n");
            int rank = 1;
            for (Object[] row : top) {
                RequestType type = (RequestType) row[0];
                Long count = (Long) row[1];
                sb.append(String.format("%d. %s — *%d*\n", rank++, formatType(type), count));
                if (rank > 10) break;
            }
        }
        return sb.toString();
    }

    public Path exportToCsv() throws IOException {
        List<RequestLog> logs = logRepository
                .findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime.now().minusDays(7));

        Path dir = Paths.get("reports");
        Files.createDirectories(dir);
        String filename = "report_" + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".csv";
        Path file = dir.resolve(filename);

        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(file))) {
            pw.println("id,telegram_id,request_type,success,error_message,created_at");
            for (RequestLog r : logs) {
                pw.printf("%d,%d,%s,%s,%s,%s%n",
                        r.getId(),
                        r.getTelegramId(),
                        r.getRequestType(),
                        r.getSuccess(),
                        r.getErrorMessage() != null ? r.getErrorMessage().replace(",", ";") : "",
                        r.getCreatedAt().format(DISPLAY_FMT));
            }
        }
        log.info("CSV report exported: {}", file);
        return file;
    }

    public Path exportToJson() throws IOException {
        List<RequestLog> logs = logRepository
                .findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime.now().minusDays(7));

        Path dir = Paths.get("reports");
        Files.createDirectories(dir);
        String filename = "report_" + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".json";
        Path file = dir.resolve(filename);

        ObjectMapper mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);

        List<Map<String, Object>> dtos = new ArrayList<>();
        for (RequestLog r : logs) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", r.getId());
            dto.put("telegramId", r.getTelegramId());
            dto.put("requestType", r.getRequestType().name());
            dto.put("success", r.getSuccess());
            dto.put("errorMessage", r.getErrorMessage());
            dto.put("createdAt", r.getCreatedAt().format(DISPLAY_FMT));
            dtos.add(dto);
        }
        mapper.writeValue(file.toFile(), dtos);
        log.info("JSON report exported: {}", file);
        return file;
    }

    @Scheduled(cron = "0 0 0 * * *") // midnight
    public void scheduledDailyExport() {
        try {
            Path csv  = exportToCsv();
            Path json = exportToJson();
            log.info("Daily reports generated: {} | {}", csv.getFileName(), json.getFileName());
        } catch (Exception e) {
            log.error("Failed to generate daily report: {}", e.getMessage());
        }
    }

    private String formatType(RequestType type) {
        return switch (type) {
            case WEATHER         -> "🌤 Погода";
            case CURRENCY        -> "💱 Курс валют";
            case NEWS            -> "📰 Новини";
            case TRANSLATE       -> "🌐 Переклад";
            case REMINDER_CREATE -> "🔔 Нагадування";
            case REMINDER_LIST   -> "📋 Список нагадувань";
            case REMINDER_DELETE -> "🗑 Видалення нагадування";
            case EVENT_CREATE    -> "📅 Нова подія";
            case EVENT_LIST      -> "📅 Список подій";
            case EVENT_TODAY     -> "📅 Події сьогодні";
            case EVENT_DELETE    -> "🗑 Видалення події";
            case PROFILE         -> "👤 Профіль";
            case SETTINGS        -> "⚙️ Налаштування";
            case HELP            -> "❓ Допомога";
            case START           -> "🚀 Старт";
            default              -> "❓ Інше";
        };
    }
}
