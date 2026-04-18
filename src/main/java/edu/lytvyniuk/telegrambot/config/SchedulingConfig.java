package edu.lytvyniuk.telegrambot.config;

/*
  @author darin
  @project telegram-bot
  @class SchedulingConfig
  @version 1.0.0
  @since 18.04.2026 - 15.32
*/

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Enables @Scheduled annotation support throughout the application.
    // ReminderService uses @Scheduled(fixedDelay = 30_000) to fire due reminders.
}