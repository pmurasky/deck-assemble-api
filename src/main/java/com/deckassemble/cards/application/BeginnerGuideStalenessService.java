package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.BeginnerGuideRepository;
import com.deckassemble.cards.domain.BeginnerGuideSource;
import com.deckassemble.cards.domain.BeginnerGuideStatus;
import com.deckassemble.cards.domain.Card;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Marks published beginner guides stale when their card oracle source changes. */
@Service
public class BeginnerGuideStalenessService {
    private final BeginnerGuideRepository guideRepository;

    public BeginnerGuideStalenessService(BeginnerGuideRepository guideRepository) {
        this.guideRepository = guideRepository;
    }

    /** Marks the card's published guide stale when its stored oracle hash is outdated. */
    @Transactional
    public void markStaleIfOracleChanged(Card card) {
        var currentHash = BeginnerGuideSource.fromCard(card, List.of()).oracleHash();
        guideRepository.findById(card.getId()).stream()
                .filter(guide -> guide.getStatus() == BeginnerGuideStatus.PUBLISHED)
                .filter(guide -> !currentHash.equals(guide.getSourceOracleHash()))
                .forEach(
                        guide -> {
                            guide.markStale();
                            guideRepository.save(guide);
                        });
    }
}
