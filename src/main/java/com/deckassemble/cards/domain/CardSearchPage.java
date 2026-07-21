package com.deckassemble.cards.domain;

import java.net.URI;
import java.util.List;

public record CardSearchPage(List<CardImportData> data, boolean hasMore, URI nextPage) {}
