package edu.lytvyniuk.telegrambot.config;

import edu.lytvyniuk.telegrambot.entity.UserProfile;
import edu.lytvyniuk.telegrambot.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProfileCommandHandler {

    private static final Set<String> VALID_TIME_FORMATS = Set.of("12h", "24h");
    private static final Set<String> VALID_LANGUAGES = Set.of("uk", "en");

    private final UserProfileService userProfileService;

    public String handleProfile(Long telegramId) {
        UserProfile p = userProfileService.getProfile(telegramId);
        if (p == null) return "Профіль не знайдено.";

        return String.format("""
                *Ваш профіль:*
                
                Ім'я: *%s*
                Улюблене місто: *%s*
                Формат часу: *%s*
                
                _Команди для оновлення:_
                `/setcity Київ` — змінити місто
                `/settimeformat 12h` — змінити формат часу
                `/settings` — всі налаштування
                """,
                p.getFirstName() != null ? p.getFirstName() : "—",
                p.getFavoriteCity() != null ? p.getFavoriteCity() : "не встановлено",
                p.getTimeFormat()
        );
    }

    public String handleSetCity(Long telegramId, String city) {
        if (city.isBlank())
            return "Вкажіть місто. Приклад: `/setcity Київ`";
        userProfileService.setCity(telegramId, city);
        return String.format(
                "Улюблене місто змінено на *%s*. Тепер `/weather` використовуватиме його за замовчуванням.", city
        );
    }

    public String handleSetTimeFormat(Long telegramId, String format) {
        if (!VALID_TIME_FORMATS.contains(format.toLowerCase()))
            return "Доступні формати: `12h`, `24h`";
        userProfileService.setTimeFormat(telegramId, format.toLowerCase());
        return String.format("Формат часу змінено на *%s*.", format.toLowerCase());
    }

    public String handleSettings(Long telegramId) {
        UserProfile p = userProfileService.getProfile(telegramId);
        if (p == null) return "Профіль не знайдено.";

        return String.format("""
                *Налаштування профілю:*
                
                *Ім'я:* %s
                *Улюблене місто:* %s
                *Формат часу:* %s
                
                *Доступні команди:*
                `/setcity [місто]` — встановити улюблене місто
                `/settimeformat [12h/24h]` — формат часу
                `/profile` — переглянути профіль
                """,
                p.getFirstName() != null ? p.getFirstName() : "—",
                p.getFavoriteCity() != null ? p.getFavoriteCity() : "не встановлено",
                p.getTimeFormat()
        );
    }
}