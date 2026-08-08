package com.deckassemble.decks.domain.organization;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** A profile-owned folder used to group decks. A deck belongs to at most one folder. */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "deck_folders")
public class DeckFolder extends ProfileOwnedNamedEntity {

    protected DeckFolder() {}

    public DeckFolder(Long profileId, String name) {
        super(profileId, name);
    }
}
