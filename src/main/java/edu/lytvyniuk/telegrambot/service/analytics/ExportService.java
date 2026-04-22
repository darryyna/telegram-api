package edu.lytvyniuk.telegrambot.service.analytics;

/*
  @author darin
  @project telegram-bot
  @class ExportService
  @version 1.0.0
  @since 22.04.2026 - 19.10
*/
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.lytvyniuk.telegrambot.entity.logs.RequestLog;
import edu.lytvyniuk.telegrambot.entity.logs.RequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final RequestLogRepository logRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final int LAST_DAYS = 7;

    public byte[] exportCsv(Long telegramId) {
        List<RequestLog> logs = fetchUserLogs(telegramId, LAST_DAYS);

        StringBuilder sb = new StringBuilder();
        sb.append("ID,Telegram ID,Request Type,Success,Error Message,Created At\n");

        for (RequestLog log : logs) {
            String errorMsg = log.getErrorMessage() != null
                    ? log.getErrorMessage().replace(",", ";").replace("\n", " ")
                    : "";

            sb.append(log.getId()).append(",")
                    .append(log.getTelegramId()).append(",")
                    .append(log.getRequestType().name()).append(",")
                    .append(log.getSuccess()).append(",")
                    .append("\"").append(errorMsg).append("\",")
                    .append(log.getCreatedAt().format(DISPLAY_FMT))
                    .append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportJson(Long telegramId) {
        List<RequestLog> logs = fetchUserLogs(telegramId, LAST_DAYS);

        List<Map<String, Object>> dtos = new ArrayList<>();

        for (RequestLog log : logs) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", log.getId());
            dto.put("telegramId", log.getTelegramId());
            dto.put("requestType", log.getRequestType().name());
            dto.put("success", log.getSuccess());
            dto.put("errorMessage", log.getErrorMessage());
            dto.put("createdAt", log.getCreatedAt().format(DISPLAY_FMT));
            dtos.add(dto);
        }

        try {
            return objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(dtos);
        } catch (Exception e) {
            log.error("JSON export failed for user {}: {}", telegramId, e.getMessage());
            return "[]".getBytes(StandardCharsets.UTF_8);
        }
    }

    private List<RequestLog> fetchUserLogs(Long telegramId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return logRepository.findByTelegramIdAndCreatedAtAfterOrderByCreatedAtDesc(
                telegramId, since
        );
    }
}
