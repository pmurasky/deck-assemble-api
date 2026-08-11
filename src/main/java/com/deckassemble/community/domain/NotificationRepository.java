package com.deckassemble.community.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(Long recipientId);

    List<Notification> findByRecipientIdOrderByReadAtAscCreatedAtDesc(Long recipientId);

    Optional<Notification> findByIdAndRecipientId(UUID id, Long recipientId);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);
}
