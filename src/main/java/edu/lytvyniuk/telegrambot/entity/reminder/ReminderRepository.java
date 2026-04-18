package edu.lytvyniuk.telegrambot.entity.reminder;

/*
  @author darin
  @project telegram-bot
  @class ReminderRepository
  @version 1.0.0
  @since 18.04.2026 - 15.19
*/
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByTelegramIdAndSentFalseOrderByRemindAtAsc(Long telegramId);

    @Query("SELECT r FROM Reminder r WHERE r.sent = false AND r.remindAt <= :now")
    List<Reminder> findPendingReminders(LocalDateTime now);

    List<Reminder> findByTelegramIdOrderByRemindAtDesc(Long telegramId);
}