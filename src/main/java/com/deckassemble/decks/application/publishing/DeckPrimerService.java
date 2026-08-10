package com.deckassemble.decks.application.publishing;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.history.DeckChangeType;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeckPrimerService {

    private final DeckRepository deckRepository;
    private final DeckAccessGuard deckAccessGuard;
    private final DeckRevisionService deckRevisionService;

    public DeckPrimerService(
            DeckRepository deckRepository,
            DeckAccessGuard deckAccessGuard,
            DeckRevisionService deckRevisionService) {
        this.deckRepository = deckRepository;
        this.deckAccessGuard = deckAccessGuard;
        this.deckRevisionService = deckRevisionService;
    }

    public PrimerResult updatePrimer(
            long deckId, String title, String markdownSource, @Nullable Integer expectedRevision) {
        Deck deck = deckAccessGuard.editableLocked(deckId);
        deckRevisionService.assertExpectedRevision(deckId, expectedRevision);
        boolean changed =
                !title.equals(deck.getPrimerTitle())
                        || !markdownSource.equals(deck.getPrimerMarkdown());
        deck.setPrimerTitle(title);
        deck.setPrimerMarkdown(markdownSource);
        Deck saved = deckRepository.save(deck);
        // ponytail: the primer is deliberately excluded from the deck snapshot (see
        // DeckPublishingService#publish), so this revision carries no primer diff — it exists only
        // to
        // advance the revision counter, which is what makes a second concurrent primer edit from
        // the
        // same base revision lose the expectedRevision check.
        if (changed) {
            deckRevisionService.record(
                    saved, deckAccessGuard.profileId(), DeckChangeType.METADATA_UPDATED);
        }
        return new PrimerResult(saved, deckRevisionService.currentRevisionNumberUnchecked(deckId));
    }

    /** The updated deck plus its resulting revision number, for the primer response. */
    public record PrimerResult(Deck deck, int revisionNumber) {}
}
