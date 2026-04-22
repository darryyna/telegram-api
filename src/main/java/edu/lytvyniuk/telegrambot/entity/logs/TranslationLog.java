package edu.lytvyniuk.telegrambot.entity.logs;

/*
  @author darin
  @project telegram-bot
  @class TranslationLog
  @version 1.0.0
  @since 18.04.2026 - 16.29
*/
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "translation_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_id", nullable = false)
    private Long telegramId;

    @Column(name = "source_text", nullable = false, length = 2000)
    private String sourceText;

    @Column(name = "translated_text", length = 2000)
    private String translatedText;

    @Column(name = "source_lang", length = 10)
    private String sourceLang;

    @Column(name = "target_lang", nullable = false, length = 10)
    private String targetLang;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
