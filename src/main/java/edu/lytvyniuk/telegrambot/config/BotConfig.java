package edu.lytvyniuk.telegrambot.config;

import edu.lytvyniuk.telegrambot.bot.TelegramBot;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/*
  @author darin
  @project telegram-bot
  @class BotConfig
  @version 1.0.0
  @since 18.03.2026 - 23.31
*/
public class BotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBot telegramBot) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(telegramBot);
        return api;
    }
}