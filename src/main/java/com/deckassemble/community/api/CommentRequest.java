package com.deckassemble.community.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Comment body for creating or editing a deck comment. Max length matches deck description's. */
public record CommentRequest(@NotBlank @Size(max = 2000) String body) {}
