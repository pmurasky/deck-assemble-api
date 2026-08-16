package com.deckassemble.imports.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.ScryfallClient;
import com.deckassemble.shared.security.CurrentUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OracleTagImportService {

    public static final String TAGGER_QUERY = "tagger:oracle-tags";

    private final ScryfallClient scryfallClient;
    private final CardRepository cardRepository;
    private final ImportRunRecorder runRecorder;
    private final CurrentUser currentUser;

    public OracleTagImportService(
            ScryfallClient scryfallClient,
            CardRepository cardRepository,
            ImportRunRecorder runRecorder,
            CurrentUser currentUser) {
        this.scryfallClient = scryfallClient;
        this.cardRepository = cardRepository;
        this.runRecorder = runRecorder;
        this.currentUser = currentUser;
    }

    @Transactional
    public ImportResult importTags() {
        long runId = runRecorder.start(TAGGER_QUERY, currentUser.subject().orElse("system"));
        return importTags(runId);
    }

    @Transactional
    public ImportResult importTags(long runId) {
        try {
            ImportResult result = applyTags(scryfallClient.fetchOracleTagAssignments(), runId);
            runRecorder.complete(
                    runId,
                    result.recordsRead(),
                    result.recordsCreated(),
                    result.recordsUpdated(),
                    result.recordsFailed());
            return result;
        } catch (RuntimeException exception) {
            runRecorder.fail(runId, String.valueOf(exception.getMessage()));
            throw exception;
        }
    }

    private ImportResult applyTags(Map<String, Set<String>> index, long runId) {
        Map<String, Card> cardsByOracleId =
                cardRepository.findAll().stream()
                        .collect(Collectors.toMap(Card::getScryfallOracleId, Function.identity()));
        List<Card> changed = applyTagsToCards(cardsByOracleId, index);
        cardRepository.saveAll(changed);
        return new ImportResult(runId, cardsByOracleId.size(), 0, changed.size(), 0);
    }

    private List<Card> applyTagsToCards(
            Map<String, Card> cardsByOracleId, Map<String, Set<String>> index) {
        List<Card> changed = new ArrayList<>();
        for (Card card : cardsByOracleId.values()) {
            Set<String> labels = index.get(card.getScryfallOracleId());
            String tags = labels == null ? null : String.join(",", new TreeSet<>(labels));
            if (!Objects.equals(tags, card.getOracleTags())) {
                card.setOracleTags(tags);
                changed.add(card);
            }
        }
        return changed;
    }
}
