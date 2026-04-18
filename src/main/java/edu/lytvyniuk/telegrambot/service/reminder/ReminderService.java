package edu.lytvyniuk.telegrambot.service.reminder;

/*
  @author darin
  @project telegram-bot
  @class ReminderService
  @version 1.0.0
  @since 18.04.2026 - 15.21
*/
import edu.lytvyniuk.telegrambot.entity.reminder.Reminder;
import edu.lytvyniuk.telegrambot.entity.reminder.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private AbsSender bot;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final ZoneId KYIV_ZONE = ZoneId.of("Europe/Kyiv");

    public void setBot(AbsSender bot) {
        this.bot = bot;
    }

    @Transactional
    public Reminder create(Long telegramId, String chatId, String message, LocalDateTime remindAt) {
        Reminder reminder = Reminder.builder()
                .telegramId(telegramId)
                .chatId(chatId)
                .message(message)
                .remindAt(remindAt)
                .build();
        Reminder saved = reminderRepository.save(reminder);
        log.info("Reminder #{} created for user {} at {}", saved.getId(), telegramId, remindAt);
        return saved;
    }

    public List<Reminder> listActive(Long telegramId) {
        return reminderRepository.findByTelegramIdAndSentFalseOrderByRemindAtAsc(telegramId);
    }

    public List<Reminder> listAll(Long telegramId) {
        return reminderRepository.findByTelegramIdOrderByRemindAtDesc(telegramId);
    }

    @Transactional
    public boolean delete(Long reminderId, Long telegramId) {
        Optional<Reminder> opt = reminderRepository.findById(reminderId);
        if (opt.isPresent() && opt.get().getTelegramId().equals(telegramId)) {
            reminderRepository.deleteById(reminderId);
            return true;
        }
        return false;
    }

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void fireReminders() {
        LocalDateTime now = LocalDateTime.now(KYIV_ZONE);
        List<Reminder> due = reminderRepository.findPendingReminders(now);
        for (Reminder r : due) {
            sendReminderNotification(r);
            r.setSent(true);
            reminderRepository.save(r);
        }
    }

    private void sendReminderNotification(Reminder r) {
        if (bot == null) {
            log.warn("Bot sender not set – cannot send reminder #{}", r.getId());
            return;
        }
        String text = String.format("""
                *Нагадування!*

                %s

                _Заплановано на: %s_
                """, r.getMessage(), r.getRemindAt().format(DISPLAY_FMT));

        SendMessage msg = SendMessage.builder()
                .chatId(r.getChatId())
                .text(text)
                .parseMode("Markdown")
                .build();
        try {
            bot.execute(msg);
            log.info("Reminder #{} sent to chat {}", r.getId(), r.getChatId());
        } catch (Exception e) {
            log.error("Failed to send reminder #{}: {}", r.getId(), e.getMessage());
        }
    }

    public String formatReminderList(List<Reminder> reminders) {
        if (reminders.isEmpty()) return "У вас немає активних нагадувань.";
        StringBuilder sb = new StringBuilder("*Ваші нагадування:*\n\n");
        for (Reminder r : reminders) {
            sb.append(String.format("*#%d* — %s\n    %s\n\n",
                    r.getId(),
                    r.getMessage(),
                    r.getRemindAt().format(DISPLAY_FMT)));
        }
        sb.append("Для видалення: `/deletereminder <id>`");
        return sb.toString();
    }
}
