package com.deckassemble.community.domain;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationDedupeKeyRepository
        extends JpaRepository<NotificationDedupeKey, String> {

    @Modifying
    @Query(
            value =
                    """
                    insert into notification_dedupe_keys (dedupe_key, last_notification_at)
                    values (:id, :lastNotificationAt)
                    on conflict (dedupe_key) do nothing
                    """,
            nativeQuery = true)
    int insertIfAbsent(
            @Param("id") String id, @Param("lastNotificationAt") Instant lastNotificationAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<NotificationDedupeKey> findLockedById(String id);
}
