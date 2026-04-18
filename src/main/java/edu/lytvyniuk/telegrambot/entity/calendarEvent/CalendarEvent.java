package edu.lytvyniuk.telegrambot.entity.calendarEvent;

/*
  @author darin
  @project telegram-bot
  @class CalendarEvent
  @version 1.0.0
  @since 18.04.2026 - 15.20
*/
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "calendar_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_id", nullable = false)
    private Long telegramId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "event_start", nullable = false)
    private LocalDateTime eventStart;

    @Column(name = "event_end")
    private LocalDateTime eventEnd;

    @Column(name = "google_event_id")
    private String googleEventId;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}