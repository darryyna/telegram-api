package edu.lytvyniuk.telegrambot.bot;

import edu.lytvyniuk.telegrambot.DTO.CurrencyResponse;
import edu.lytvyniuk.telegrambot.DTO.NewsResponse;
import edu.lytvyniuk.telegrambot.DTO.NlpResult;
import edu.lytvyniuk.telegrambot.DTO.WeatherResponse;
import edu.lytvyniuk.telegrambot.config.ProfileCommandHandler;
import edu.lytvyniuk.telegrambot.entity.UserProfile;
import edu.lytvyniuk.telegrambot.service.MessageFormatterService;
import edu.lytvyniuk.telegrambot.service.NlpService;
import edu.lytvyniuk.telegrambot.service.SmartApiService;
import edu.lytvyniuk.telegrambot.service.UserProfileService;
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

import java.util.List;

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

    @Autowired
    private SmartApiService smartApiService;

    @Autowired
    private MessageFormatterService formatter;

    @Autowired
    private NlpService nlpService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private ProfileCommandHandler profileCommandHandler;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @PostConstruct
    public void registerBot() {
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);
            log.info("Bot '{}' registered successfully", botUsername);
        } catch (TelegramApiException e) {
            log.error("Failed to register bot: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Message message = update.getMessage();
        String chatId    = message.getChatId().toString();
        String text      = message.getText().trim();
        Long   userId    = message.getFrom().getId();
        String firstName = message.getFrom().getFirstName();
        String username  = message.getFrom().getUserName();
        userProfileService.getOrCreate(userId, firstName, username);

        log.info("Received message from {} ({}): {}", firstName, chatId, text);

        String response = text.startsWith("/")
                ? handleCommand(text, firstName, userId)
                : handleFreeText(text, userId);

        sendReply(chatId, response);
    }

    private String handleCommand(String text, String firstName, Long userId) {
        String cleanText = text.replaceFirst("@\\S+", "").trim();

        if (cleanText.equalsIgnoreCase("/start"))              return buildStartMessage(firstName);
        if (cleanText.equalsIgnoreCase("/help"))               return buildHelpMessage();
        if (cleanText.equalsIgnoreCase("/profile"))            return profileCommandHandler.handleProfile(userId);
        if (cleanText.equalsIgnoreCase("/settings"))           return profileCommandHandler.handleSettings(userId);
        if (cleanText.toLowerCase().startsWith("/setcity"))    return profileCommandHandler.handleSetCity(userId, extractArgument(cleanText, "/setcity"));
        if (cleanText.toLowerCase().startsWith("/settimeformat")) return profileCommandHandler.handleSetTimeFormat(userId, extractArgument(cleanText, "/settimeformat"));
        if (cleanText.toLowerCase().startsWith("/weather"))    return handleWeatherCommand(cleanText, userId);
        if (cleanText.toLowerCase().startsWith("/currency"))   return handleCurrencyCommand(cleanText);
        if (cleanText.toLowerCase().startsWith("/news"))       return handleNewsCommand(cleanText);

        return "Unknown command. Type /help to see available commands.";
    }

    private String handleWeatherCommand(String text, Long userId) {
        String city = extractArgument(text, "/weather");
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
        String base = extractArgument(text, "/currency").toUpperCase();
        if (base.isEmpty()) base = "USD";
        return formatter.formatCurrency(smartApiService.getCurrency(base));
    }

    private String handleNewsCommand(String text) {
        String category = extractArgument(text, "/news").toLowerCase();
        if (category.isEmpty()) category = "general";
        return formatter.formatNews(smartApiService.getNews(category));
    }

    private String handleFreeText(String text, Long userId) {
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
                I didn't understand your request 🤔
                
                Try:
                - *weather London*
                - *rate EUR*
                - *news technology*
                
                Or type /help for all commands.
                """;
        };
    }

    private String extractArgument(String text, String command) {
        if (text.length() <= command.length()) return "";
        return text.substring(command.length()).trim();
    }

    private String buildStartMessage(String firstName) {
        return String.format("""
            Привіт, *%s*! Я SmartBot.
            
            Я можу допомогти з:
            - Погода: `/weather Київ`
            - Курси валют: `/currency USD`
            - Новини: `/news технології`
            
            Можна також писати звичайним текстом:
            - _"погода в Берліні"_
            - _"курс євро"_
            - _"останні новини технологій"_
            
            Збережи своє місто: `/setcity Київ`
            Переглянь профіль: `/profile`
            Введи /help для списку команд.
            """, firstName);
    }


    private String buildHelpMessage() {
        return """
            *Доступні команди:*
            
            `/start` — Вітальне повідомлення
            
            `/weather [місто]` — Поточна погода
              _Приклад: /weather Львів_
              _Без міста — використовує збережене місто_
            
            `/currency [код]` — Курси валют
              _Приклад: /currency EUR_
            
            `/news [категорія]` — Останні новини
              _Категорії: general, technology, sports, business, science, health, entertainment_
              _Приклад: /news sports_
            
            `/profile` — Переглянути профіль
            `/settings` — Налаштування профілю
            `/setcity [місто]` — Встановити улюблене місто
            `/settimeformat [12h/24h]` — Формат часу
            `/setlang [uk/en]` — Змінити мову
            
            `/help` — Це повідомлення
            """;
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
}