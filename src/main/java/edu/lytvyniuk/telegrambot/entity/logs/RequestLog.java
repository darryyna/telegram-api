package edu.lytvyniuk.telegrambot.entity.logs;

/*
  @author darin
  @project telegram-bot
  @class RequestLog
  @version 1.0.0
  @since 18.04.2026 - 16.29
*/
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "request_logs", indexes = {
        @Index(name = "idx_request_logs_telegram_id", columnList = "telegram_id"),
        @Index(name = "idx_request_logs_created_at",  columnList = "created_at"),
        @Index(name = "idx_request_logs_request_type", columnList = "request_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_id", nullable = false)
    private Long telegramId;

    @Column(name = "chat_id")
    private String chatId;

    @Column(name = "raw_text", length = 2000)
    private String rawText;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 30)
    private RequestType requestType;

    @Column(name = "success", nullable = false)
    @Builder.Default
    private Boolean success = true;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
