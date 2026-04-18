package edu.lytvyniuk.telegrambot.service;

/*
  @author darin
  @project telegram-bot
  @class UserProfileService
  @version 1.0.0
  @since 15.04.2026 - 23.18
*/

import edu.lytvyniuk.telegrambot.entity.user.UserProfile;
import edu.lytvyniuk.telegrambot.entity.user.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository repository;

    @Transactional
    public UserProfile getOrCreate(Long telegramId, String firstName, String username) {
        return repository.findByTelegramId(telegramId)
                .orElseGet(() -> {
                    UserProfile profile = UserProfile.builder()
                            .telegramId(telegramId)
                            .firstName(firstName)
                            .username(username)
                            .build();
                    log.info("Creating new profile for user {}", telegramId);
                    return repository.save(profile);
                });
    }

    public UserProfile getProfile(Long telegramId) {
        return repository.findByTelegramId(telegramId).orElse(null);
    }

    @Transactional
    public UserProfile setCity(Long telegramId, String city) {
        return repository.findByTelegramId(telegramId).map(p -> {
            p.setFavoriteCity(city);
            return repository.save(p);
        }).orElseThrow();
    }

    @Transactional
    public UserProfile setTimeFormat(Long telegramId, String format) {
        return repository.findByTelegramId(telegramId).map(p -> {
            p.setTimeFormat(format);
            return repository.save(p);
        }).orElseThrow();
    }
}
