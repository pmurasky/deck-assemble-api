package com.deckassemble.community.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileFollowRepository extends JpaRepository<ProfileFollow, UUID> {

    List<ProfileFollow> findByFollowerId(Long followerId);

    List<ProfileFollow> findByFolloweeId(Long followeeId);

    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
}
