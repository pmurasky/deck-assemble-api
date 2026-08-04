package com.deckassemble.users.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByAuthProviderSubject(String authProviderSubject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT profile FROM Profile profile WHERE profile.id = :profileId")
    Optional<Profile> findLockedById(Long profileId);
}
