package edu.lytvyniuk.telegrambot.bot;

import edu.lytvyniuk.telegrambot.DTO.NlpResult;
import edu.lytvyniuk.telegrambot.config.handlers.CalendarCommandHandler;
import edu.lytvyniuk.telegrambot.config.handlers.ProfileCommandHandler;
import edu.lytvyniuk.telegrambot.config.handlers.ReminderCommandHandler;
import edu.lytvyniuk.telegrambot.entity.user.UserProfile;
import edu.lytvyniuk.telegrambot.service.*;
import edu.lytvyniuk.telegrambot.service.reminder.ReminderService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/*
  @author darin
  @project telegram-bot
  @class TelegramBot
  @version 1.0.0
  @since 18.03.2026
*/
@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired private SmartApiService smartApiService;
    @Autowired private MessageFormatterService formatter;
    @Autowired private NlpService nlpService;
    @Autowired private UserProfileService userProfileService;
    @Autowired private ProfileCommandHandler profileCommandHandler;
    @Autowired private ReminderCommandHandler reminderCommandHandler;
    @Autowired private CalendarCommandHandler calendarCommandHandler;
    @Autowired private ReminderService reminderService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @PostConstruct
    public void registerBot() {
        reminderService.setBot(this);
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);
            log.info("Bot '{}' registered successfully", botUsername);
        } catch (TelegramApiException e) {
            log.error("Failed to register bot: {}", e.getMessage(), e);
        }
    }

    @Override public String getBotUsername() { return botUsername; }
    @Override public String getBotToken()    { return botToken; }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Message message  = update.getMessage();
        String  chatId   = message.getChatId().toString();
        String  text     = message.getText().trim();
        Long    userId   = message.getFrom().getId();
        String  firstName = message.getFrom().getFirstName();
        String  username  = message.getFrom().getUserName();
        userProfileService.getOrCreate(userId, firstName, username);

        log.info("Received from {} ({}): {}", firstName, chatId, text);

        String response = text.startsWith("/")
                ? handleCommand(text, firstName, userId, chatId)
                : handleFreeText(text, userId, chatId);

        sendReply(chatId, response);
    }

    private String handleCommand(String text, String firstName, Long userId, String chatId) {
        String clean = text.replaceFirst("@\\S+", "").trim();

        if (clean.equalsIgnoreCase("/start"))           return buildStartMessage(firstName);
        if (clean.equalsIgnoreCase("/help"))            return buildHelpMessage();
        if (clean.equalsIgnoreCase("/profile"))         return profileCommandHandler.handleProfile(userId);
        if (clean.equalsIgnoreCase("/settings"))        return profileCommandHandler.handleSettings(userId);

        if (clean.toLowerCase().startsWith("/setcity"))
            return profileCommandHandler.handleSetCity(userId, extractArg(clean, "/setcity"));
        if (clean.toLowerCase().startsWith("/settimeformat"))
            return profileCommandHandler.handleSetTimeFormat(userId, extractArg(clean, "/settimeformat"));
        if (clean.toLowerCase().startsWith("/weather"))
            return handleWeatherCommand(clean, userId);
        if (clean.toLowerCase().startsWith("/currency"))
            return handleCurrencyCommand(clean);
        if (clean.toLowerCase().startsWith("/news"))
            return handleNewsCommand(clean);

        if (clean.toLowerCase().startsWith("/reminder"))
            return reminderCommandHandler.handleCreate(userId, chatId, extractArg(clean, "/reminder"));
        if (clean.equalsIgnoreCase("/reminders"))
            return reminderCommandHandler.handleList(userId);
        if (clean.toLowerCase().startsWith("/deletereminder"))
            return reminderCommandHandler.handleDelete(userId, extractArg(clean, "/deletereminder"));

        if (clean.toLowerCase().startsWith("/addevent"))
            return calendarCommandHandler.handleCreate(userId, extractArg(clean, "/addevent"));
        if (clean.equalsIgnoreCase("/events"))
            return calendarCommandHandler.handleUpcoming(userId);
        if (clean.equalsIgnoreCase("/today"))
            return calendarCommandHandler.handleToday(userId);
        if (clean.toLowerCase().startsWith("/deleteevent"))
            return calendarCommandHandler.handleDelete(userId, extractArg(clean, "/deleteevent"));

        return "Unknown command. Type /help to see available commands.";
    }

    private String handleFreeText(String text, Long userId, String chatId) {
        String lower = text.toLowerCase();

        if (lower.contains("нагадай") || lower.contains("remind me") || lower.contains("remind")) {
            return reminderCommandHandler.handleCreate(userId, chatId, text);
        }

        if (lower.contains("створи подію") || lower.contains("додай подію")
                || lower.contains("create event") || lower.contains("add event")) {
            return calendarCommandHandler.handleCreate(userId, text);
        }

        NlpResult result = nlpService.analyze(text);
        return switch (result.getIntent()) {
            case WEATHER -> {
                String city = result.getLocation();
                if (city == null || city.isEmpty()) {
                    UserProfile profile = userProfileService.getProfile(userId);
                    if (profile != null && profile.getFavoriteCity() != null) {
                        yield formatter.formatWeather(smartApiService.getWeather(profile.getFavoriteCity()));
                    }
                    yield "Which city? \nExample: *weather London* or `/weather London`";
                }
                yield formatter.formatWeather(smartApiService.getWeather(city));
            }
            case CURRENCY -> formatter.formatCurrency(smartApiService.getCurrency(result.getCurrencyCode()));
            case NEWS     -> formatter.formatNews(smartApiService.getNews(result.getNewsCategory()));
            case HELP     -> buildHelpMessage();
            case UNKNOWN  -> """
                I didn't understand your request

                Try:
                - *weather London*
                - *rate EUR*
                - *news technology*
                - *нагадай завтра о 9:00 зустріч*
                - *створи подію на понеділок о 14:00 Мітинг*

                Or type /help for all commands.
                """;
        };
    }

    private String handleWeatherCommand(String text, Long userId) {
        String city = extractArg(text, "/weather");
        if (city.isEmpty()) {
            UserProfile profile = userProfileService.getProfile(userId);
            if (profile != null && profile.getFavoriteCity() != null) {
                city = profile.getFavoriteCity();
            } else {
                return "Please specify a city or save your city with `/setcity London`";
            }
        }
        return formatter.formatWeather(smartApiService.getWeather(city));
    }

    private String handleCurrencyCommand(String text) {
        String base = extractArg(text, "/currency").toUpperCase();
        if (base.isEmpty()) base = "USD";
        return formatter.formatCurrency(smartApiService.getCurrency(base));
    }

    private String handleNewsCommand(String text) {
        String category = extractArg(text, "/news").toLowerCase();
        if (category.isEmpty()) category = "general";
        return formatter.formatNews(smartApiService.getNews(category));
    }

    private String extractArg(String text, String command) {
        if (text.length() <= command.length()) return "";
        return text.substring(command.length()).trim();
    }

    private void sendReply(String chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .build();
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to {}: {}", chatId, e.getMessage());
        }
    }

    private String buildStartMessage(String firstName) {
        return String.format("""
            Привіт, *%s*! Я SmartBot.
            
            Я можу допомогти з:
            - Погода: `/weather Київ`
            - Курси валют: `/currency USD`
            - Новини: `/news технології`
            - Нагадування: `/reminder завтра о 9:00 зустріч`
            - Календар: `/addevent понеділок о 14:00 Нарада`
            
            Введи /help для повного списку команд.
            """, firstName);
    }

    private String buildHelpMessage() {
        return """
            *Доступні команди:*
            
            `/start` — Вітальне повідомлення
            `/weather [місто]` — Поточна погода
            `/currency [код]` — Курси валют
            `/news [категорія]` — Останні новини
            `/profile` — Переглянути профіль
            `/settings` — Налаштування профілю
            `/setcity [місто]` — Встановити улюблене місто
            `/settimeformat [12h/24h]` — Формат часу
            
            *🔔 Нагадування:*
            `/reminder [текст]` — Створити нагадування
              _Приклади:_
              `/reminder завтра о 9:00 Зустріч`
              `/reminder через 30 хвилин кава`
              `/reminder 25.04 о 14:30 Лікар`
            `/reminders` — Переглянути активні нагадування
            `/deletereminder [id]` — Видалити нагадування
            
            *📅 Календар:*
            `/addevent [текст]` — Додати подію
              _Приклади:_
              `/addevent понеділок о 14:00 Нарада`
              `/addevent завтра о 10:00 Лікар`
              `/addevent 25.04 о 09:30 Презентація`
            `/events` — Найближчі 7 днів
            `/today` — Сьогоднішні події
            `/deleteevent [id]` — Видалити подію
            
            `/help` — Це повідомлення
            """;
    }
}