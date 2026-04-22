package edu.lytvyniuk.telegrambot.entity.logs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/*
  @author darin
  @project telegram-bot
  @class RequestLogRepository
  @version 2.0.0
  @since 22.04.2026
*/

@Repository
public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {
    long countByCreatedAtAfter(LocalDateTime since);

    long countByTelegramIdAndCreatedAtAfter(Long telegramId, LocalDateTime since);

    @Query("""
            SELECT r.requestType, COUNT(r) AS cnt
            FROM RequestLog r
            WHERE r.createdAt >= :since
            GROUP BY r.requestType
            ORDER BY cnt DESC
            """)
    List<Object[]> topCommandsSince(@Param("since") LocalDateTime since);

    @Query("""
            SELECT r.requestType, COUNT(r) AS cnt
            FROM RequestLog r
            WHERE r.telegramId = :telegramId AND r.createdAt >= :since
            GROUP BY r.requestType
            ORDER BY cnt DESC
            """)
    List<Object[]> topCommandsByUser(@Param("telegramId") Long telegramId,
                                     @Param("since") LocalDateTime since);

    @Query("""
            SELECT r FROM RequestLog r
            WHERE r.success = false AND r.createdAt >= :since
            ORDER BY r.createdAt DESC
            """)
    List<RequestLog> findErrorsSince(@Param("since") LocalDateTime since);

    List<RequestLog> findByTelegramIdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long telegramId, LocalDateTime createdAfter);

    List<RequestLog> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime since);

    long countBySuccessFalseAndCreatedAtAfter(LocalDateTime since);
    long countByTelegramId(Long telegramId);
}