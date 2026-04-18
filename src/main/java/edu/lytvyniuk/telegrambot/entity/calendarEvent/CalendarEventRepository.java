package edu.lytvyniuk.telegrambot.entity.calendarEvent;

/*
  @author darin
  @project telegram-bot
  @class CalendarEventRepository
  @version 1.0.0
  @since 18.04.2026 - 15.21
*/
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    List<CalendarEvent> findByTelegramIdAndEventStartBetweenOrderByEventStartAsc(
            Long telegramId, LocalDateTime from, LocalDateTime to);

    List<CalendarEvent> findByTelegramIdAndEventStartAfterOrderByEventStartAsc(
            Long telegramId, LocalDateTime after);

    List<CalendarEvent> findByTelegramIdOrderByEventStartAsc(Long telegramId);
}
