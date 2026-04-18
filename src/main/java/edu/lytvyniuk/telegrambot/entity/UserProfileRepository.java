package edu.lytvyniuk.telegrambot.entity;

/*
  @author darin
  @project telegram-bot
  @class UserProfileRepository
  @version 1.0.0
  @since 15.04.2026 - 23.17
*/
import edu.lytvyniuk.telegrambot.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByTelegramId(Long telegramId);
}
