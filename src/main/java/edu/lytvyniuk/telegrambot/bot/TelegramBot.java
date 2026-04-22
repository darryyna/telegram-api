package edu.lytvyniuk.telegrambot.bot;

import edu.lytvyniuk.telegrambot.DTO.NlpResult;
import edu.lytvyniuk.telegrambot.config.handlers.*;
import edu.lytvyniuk.telegrambot.entity.logs.RequestType;
import edu.lytvyniuk.telegrambot.entity.user.UserProfile;
import edu.lytvyniuk.telegrambot.service.*;
import edu.lytvyniuk.telegrambot.service.analytics.AnalyticsService;
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
    @Autowired private TranslationCommandHandler translationCommandHandler;
    @Autowired private AnalyticsCommandHandler analyticsCommandHandler;
    @Autowired private AnalyticsService analyticsService;
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

        Message message   = update.getMessage();
        String  chatId    = message.getChatId().toString();
        String  text      = message.getText().trim();
        Long    userId    = message.getFrom().getId();
        String  firstName = message.getFrom().getFirstName();
        String  username  = message.getFrom().getUserName();
        userProfileService.getOrCreate(userId, firstName, username);

        log.info("Received from {} ({}): {}", firstName, chatId, text);

        String  response;
        RequestType requestType = RequestType.UNKNOWN;
        boolean success = true;
        String  errorMsg = null;

        try {
            if (text.startsWith("/")) {
                var result = handleCommand(text, firstName, userId, chatId);
                response    = result.response();
                requestType = result.type();
            } else {
                var result = handleFreeText(text, userId, chatId);
                response    = result.response();
                requestType = result.type();
            }
        } catch (Exception e) {
            log.error("Unhandled error processing message from {}: {}", userId, e.getMessage(), e);
            response  = "❌ Виникла помилка. Спробуйте пізніше.";
            success   = false;
            errorMsg  = e.getMessage();
        }

        analyticsService.log(userId, chatId, truncate(text, 300), requestType, success, errorMsg);

        sendReply(chatId, response);
    }

    private HandlerResult handleCommand(String text, String firstName, Long userId, String chatId) {
        String clean = text.replaceFirst("@\\S+", "").trim();

        if (clean.equalsIgnoreCase("/start"))
            return new HandlerResult(buildStartMessage(firstName), RequestType.START);
        if (clean.equalsIgnoreCase("/help"))
            return new HandlerResult(buildHelpMessage(), RequestType.HELP);
        if (clean.equalsIgnoreCase("/profile"))
            return new HandlerResult(profileCommandHandler.handleProfile(userId), RequestType.PROFILE);
        if (clean.equalsIgnoreCase("/settings"))
            return new HandlerResult(profileCommandHandler.handleSettings(userId), RequestType.SETTINGS);
        if (clean.equalsIgnoreCase("/stats"))
            return new HandlerResult(analyticsCommandHandler.handleUserStats(userId), RequestType.PROFILE);
        if (clean.equalsIgnoreCase("/report"))
            return new HandlerResult(analyticsCommandHandler.handleExport(), RequestType.PROFILE);

        if (clean.toLowerCase().startsWith("/setcity"))
            return new HandlerResult(profileCommandHandler.handleSetCity(userId, extractArg(clean, "/setcity")), RequestType.SETTINGS);
        if (clean.toLowerCase().startsWith("/settimeformat"))
            return new HandlerResult(profileCommandHandler.handleSetTimeFormat(userId, extractArg(clean, "/settimeformat")), RequestType.SETTINGS);

        if (clean.toLowerCase().startsWith("/weather"))
            return new HandlerResult(handleWeatherCommand(clean, userId), RequestType.WEATHER);
        if (clean.toLowerCase().startsWith("/currency"))
            return new HandlerResult(handleCurrencyCommand(clean), RequestType.CURRENCY);
        if (clean.toLowerCase().startsWith("/news"))
            return new HandlerResult(handleNewsCommand(clean), RequestType.NEWS);

        if (clean.toLowerCase().startsWith("/translate"))
            return new HandlerResult(translationCommandHandler.handle(userId, clean), RequestType.TRANSLATE);

        if (clean.toLowerCase().startsWith("/reminder"))
            return new HandlerResult(reminderCommandHandler.handleCreate(userId, chatId, extractArg(clean, "/reminder")), RequestType.REMINDER_CREATE);
        if (clean.equalsIgnoreCase("/reminders"))
            return new HandlerResult(reminderCommandHandler.handleList(userId), RequestType.REMINDER_LIST);
        if (clean.toLowerCase().startsWith("/deletereminder"))
            return new HandlerResult(reminderCommandHandler.handleDelete(userId, extractArg(clean, "/deletereminder")), RequestType.REMINDER_DELETE);

        if (clean.toLowerCase().startsWith("/addevent"))
            return new HandlerResult(calendarCommandHandler.handleCreate(userId, extractArg(clean, "/addevent")), RequestType.EVENT_CREATE);
        if (clean.equalsIgnoreCase("/events"))
            return new HandlerResult(calendarCommandHandler.handleUpcoming(userId), RequestType.EVENT_LIST);
        if (clean.equalsIgnoreCase("/today"))
            return new HandlerResult(calendarCommandHandler.handleToday(userId), RequestType.EVENT_TODAY);
        if (clean.toLowerCase().startsWith("/deleteevent"))
            return new HandlerResult(calendarCommandHandler.handleDelete(userId, extractArg(clean, "/deleteevent")), RequestType.EVENT_DELETE);

        return new HandlerResult("Unknown command. Type /help to see available commands.", RequestType.UNKNOWN);
    }

    private HandlerResult handleFreeText(String text, Long userId, String chatId) {
        String lower = text.toLowerCase();

        if (lower.contains("переклади") || lower.contains("translate to") || lower.contains("translate "))
            return new HandlerResult(translationCommandHandler.handle(userId, text), RequestType.TRANSLATE);

        if (lower.contains("нагадай") || lower.contains("remind me") || lower.contains("remind"))
            return new HandlerResult(reminderCommandHandler.handleCreate(userId, chatId, text), RequestType.REMINDER_CREATE);

        if (lower.contains("створи подію") || lower.contains("додай подію")
                || lower.contains("create event") || lower.contains("add event"))
            return new HandlerResult(calendarCommandHandler.handleCreate(userId, text), RequestType.EVENT_CREATE);

        NlpResult result = nlpService.analyze(text);
        RequestType type = switch (result.getIntent()) {
            case WEATHER  -> RequestType.WEATHER;
            case CURRENCY -> RequestType.CURRENCY;
            case NEWS     -> RequestType.NEWS;
            case HELP     -> RequestType.HELP;
            default       -> RequestType.UNKNOWN;
        };
        String response = switch (result.getIntent()) {
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
                I didn't understand your request.

                Try:
                - *weather London*
                - *rate EUR*
                - *news technology*
                - *переклади на англійську: Привіт*
                - *нагадай завтра о 9:00 зустріч*
                - *створи подію на понеділок о 14:00 Мітинг*

                Or type /help for all commands.
                """;
        };
        return new HandlerResult(response, type);
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

    private String truncate(String s, int max) {
        return s == null ? "" : (s.length() <= max ? s : s.substring(0, max));
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
            - Переклад: `/translate en Привіт`
            - Нагадування: `/reminder завтра о 9:00 зустріч`
            - Календар: `/addevent понеділок о 14:00 Нарада`
            - Статистика: `/stats`

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

            *🌐 Переклад:*
            `/translate en текст` — Перекласти на англійську
            `/translate de текст` — Перекласти на німецьку
            `/translate uk текст` — Перекласти на українську
            _Або:_ `Переклади на англійську: Привіт`

            *👤 Профіль:*
            `/profile` — Переглянути профіль
            `/settings` — Налаштування профілю
            `/setcity [місто]` — Встановити улюблене місто
            `/settimeformat [12h/24h]` — Формат часу

            *🔔 Нагадування:*
            `/reminder [текст]` — Створити нагадування
            `/reminders` — Переглянути активні нагадування
            `/deletereminder [id]` — Видалити нагадування

            *📅 Календар:*
            `/addevent [текст]` — Додати подію
            `/events` — Найближчі 7 днів
            `/today` — Сьогоднішні події
            `/deleteevent [id]` — Видалити подію

            *📊 Статистика:*
            `/stats` — Ваша особиста статистика
            `/report` — Сформувати CSV/JSON звіт

            `/help` — Це повідомлення
            """;
    }

    private record HandlerResult(String response, RequestType type) {}
}