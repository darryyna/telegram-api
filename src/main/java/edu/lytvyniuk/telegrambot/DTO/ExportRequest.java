package edu.lytvyniuk.telegrambot.DTO;

/*
  @author darin
  @project telegram-bot
  @class ExportRequest
  @version 1.0.0
  @since 22.04.2026 - 19.10
*/
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {
    public enum Format {
        CSV, JSON, BOTH
    }

    private Long telegramId;
    private Format format;
    private String chatId;
}
