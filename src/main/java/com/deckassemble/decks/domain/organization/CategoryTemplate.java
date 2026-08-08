package com.deckassemble.decks.domain.organization;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A profile-owned, reusable named list of category names that can be applied to any of the
 * profile's decks via {@link CategoryTemplateItem}, materializing user categories on that deck.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "category_templates")
public class CategoryTemplate extends ProfileOwnedNamedEntity {

    protected CategoryTemplate() {}

    public CategoryTemplate(Long profileId, String name) {
        super(profileId, name);
    }
}
