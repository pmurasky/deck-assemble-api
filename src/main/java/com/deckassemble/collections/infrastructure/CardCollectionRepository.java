package com.deckassemble.collections.infrastructure;

import com.deckassemble.collections.domain.CardCollection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardCollectionRepository extends JpaRepository<CardCollection, Long> {

    List<CardCollection> findByProfileIdOrderByNameAsc(Long profileId);

    Optional<CardCollection> findByIdAndProfileId(Long id, Long profileId);
}
