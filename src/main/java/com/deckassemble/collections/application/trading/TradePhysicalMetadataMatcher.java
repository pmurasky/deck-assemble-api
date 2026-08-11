package com.deckassemble.collections.application.trading;

import com.deckassemble.collections.domain.physical.CollectionCardPhysicalMetadata;
import com.deckassemble.collections.domain.physical.CollectionCardPhysicalMetadataRepository;
import com.deckassemble.collections.domain.trading.TradeListItem;
import org.springframework.stereotype.Component;

@Component
class TradePhysicalMetadataMatcher {

    private final CollectionCardPhysicalMetadataRepository metadataRepository;

    TradePhysicalMetadataMatcher(CollectionCardPhysicalMetadataRepository metadataRepository) {
        this.metadataRepository = metadataRepository;
    }

    private boolean hasNoMetadataFilter(TradeListItem item) {
        return item.getCondition() == null
                && item.getFinish() == null
                && item.getLanguage() == null;
    }

    boolean matches(long collectionCardId, TradeListItem item) {
        if (hasNoMetadataFilter(item)) {
            return true;
        }
        return metadataRepository.findByCollectionCardId(collectionCardId).stream()
                .anyMatch(metadata -> matches(metadata, item));
    }

    private boolean matches(CollectionCardPhysicalMetadata metadata, TradeListItem item) {
        return conditionMatches(metadata, item)
                && finishMatches(metadata, item)
                && languageMatches(metadata, item);
    }

    private boolean conditionMatches(CollectionCardPhysicalMetadata metadata, TradeListItem item) {
        return item.getCondition() == null || item.getCondition() == metadata.getCondition();
    }

    private boolean finishMatches(CollectionCardPhysicalMetadata metadata, TradeListItem item) {
        return item.getFinish() == null || item.getFinish() == metadata.getFinish();
    }

    private boolean languageMatches(CollectionCardPhysicalMetadata metadata, TradeListItem item) {
        return item.getLanguage() == null || item.getLanguage().equals(metadata.getLanguage());
    }
}
