package com.deckassemble.community.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

public class DeckDiscoveryQuery {

    private @Nullable String commander;
    private List<String> colors = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
    private @Nullable String category;
    private @Nullable Instant updatedAfter;
    private @Nullable Instant updatedBefore;
    private @Nullable Boolean favorited;

    public @Nullable String getCommander() {
        return commander;
    }

    public void setCommander(@Nullable String commander) {
        this.commander = commander;
    }

    public List<String> getColors() {
        return colors;
    }

    public void setColors(List<String> colors) {
        this.colors = colors;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public @Nullable String getCategory() {
        return category;
    }

    public void setCategory(@Nullable String category) {
        this.category = category;
    }

    public @Nullable Instant getUpdatedAfter() {
        return updatedAfter;
    }

    public void setUpdatedAfter(@Nullable Instant updatedAfter) {
        this.updatedAfter = updatedAfter;
    }

    public @Nullable Instant getUpdatedBefore() {
        return updatedBefore;
    }

    public void setUpdatedBefore(@Nullable Instant updatedBefore) {
        this.updatedBefore = updatedBefore;
    }

    public @Nullable Boolean getFavorited() {
        return favorited;
    }

    public void setFavorited(@Nullable Boolean favorited) {
        this.favorited = favorited;
    }
}
