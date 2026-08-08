package com.deckassemble.decks.domain.organization;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** A profile-owned tag. A deck may carry any number of tags. */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "deck_tags")
public class DeckTag extends ProfileOwnedNamedEntity {

    protected DeckTag() {}

    public DeckTag(Long profileId, String name) {
        super(profileId, name);
    }
}
