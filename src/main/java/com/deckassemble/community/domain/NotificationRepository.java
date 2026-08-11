package com.deckassemble.community.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(Long recipientId);

    List<Notification> findByRecipientIdOrderByReadAtAscCreatedAtDesc(Long recipientId);

    Optional<Notification> findByIdAndRecipientId(UUID id, Long recipientId);

    @Query(
            """
            select count(n) > 0 from Notification n
            where n.recipientId = :recipientId
              and n.reason = :reason
              and n.resourceId = :resourceId
              and ((:actorId is null and n.actorId is null) or n.actorId = :actorId)
              and n.readAt is null
              and n.createdAt >= :since
            """)
    boolean existsUnreadDuplicate(
            Long recipientId,
            Notification.Reason reason,
            String resourceId,
            Long actorId,
            Instant since);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);
}
