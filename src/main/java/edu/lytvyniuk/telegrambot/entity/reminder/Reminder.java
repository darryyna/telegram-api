package edu.lytvyniuk.telegrambot.entity.reminder;

/*
  @author darin
  @project telegram-bot
  @class Reminder
  @version 1.0.0
  @since 18.04.2026 - 15.19
*/
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reminders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_id", nullable = false)
    private Long telegramId;

    @Column(name = "chat_id", nullable = false)
    private String chatId;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "remind_at", nullable = false)
    private LocalDateTime remindAt;

    @Column(name = "sent")
    @Builder.Default
    private Boolean sent = false;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
