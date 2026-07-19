---
name: docusaurus-restructuring
description: >
  Use when a Docusaurus document has grown too long (300+ lines) or covers multiple
  distinct topics — restructures documentation across multiple pages while maintaining
  cross-references, sidebar ordering, navigation hierarchy, and build integrity.
  Covers page splitting, frontmatter management, category configuration, and broken
  link validation.
triggers:
  - "splitting a long docusaurus document into pages"
  - "breaking docs into smaller pages after review"
  - "reorganizing documentation with sidebar ordering preserved"
  - "separating quick start from advanced sections"
not_for:
  - "small wording or typo fixes in one page"
  - "non docusaurus docs without frontmatter and sidebars"
  - "content rewrites that keep one page structure"
---

# Docusaurus Documentation Restructuring

Systematic workflow for splitting large Docusaurus documents into multiple focused pages while preserving navigation, cross-references, and build integrity.

## Table of Contents

- [Use when](#use-when)
- [Not for](#not-for)
- [Workflow](#workflow)
- [Anti-Patterns](#anti-patterns)
- [Frontmatter Reference](#frontmatter-reference)

## Use when

- When a Docusaurus document exceeds 300 lines and covers multiple topics
- When PR reviewers request breaking a document into multiple pages
- When a document's table of contents has 10+ top-level sections
- When different audiences need different sections (Quick Start vs. Advanced Config)
- When restructuring documentation as part of a content review

## Not for

- Small wording, typo, or single-section edits that do not require splitting
- Non-Docusaurus documentation where sidebar ordering and frontmatter do not apply
- Pure content rewrites where the document structure can stay as a single page

---

## Workflow

**Step 1: Analyze** — count lines (`wc -l`), find incoming links (`grep -r "filename" docs/`), check sidebar position.

**Step 2: Plan structure** — keep the original filename for Page 1 (preserves all incoming links), use a consistent naming prefix, order by user journey (Quick Start first, FAQ last), each page independently useful.

**Step 3: Set sidebar ordering** — use `sidebar_position` in frontmatter (1=first, increments of 1). For subdirectory pages, add `_category_.json`.

**Step 4: Rewrite Page 1** — trim to essentials, keep original filename and H1 title, add cross-links to new pages, set `sidebar_position: 1`.

**Step 5: Create remaining pages** — extract content, add proper frontmatter (`title`, `sidebar_label`, `sidebar_position`, `description`), add bidirectional cross-links, ensure standalone readability. Commit each page separately.

**Step 6: Update cross-references** — find anchored links that moved to new pages and update them. Links to the original filename are unchanged (Page 1 kept it).

**Step 7: Validate** — `npm run build`. Fix new broken links/anchors; document (don't fix) pre-existing warnings.

**Step 8: Final checklist:**
- [ ] Original filename preserved
- [ ] All pages have `title`, `sidebar_label`, `sidebar_position`
- [ ] Cross-links bidirectional
- [ ] `npm run build` passes with no new warnings
- [ ] Each page under 300 lines, independently readable
- [ ] One logical change per commit

---

## Anti-Patterns

| Anti-Pattern | Do This Instead |
|-------------|-----------------|
| Rename the original file | Keep original filename for Page 1 |
| Create all pages in one commit | One page per commit |
| Assume readers read in order | Make each page standalone |
| Skip build validation | Run `npm run build` after restructuring |
| Fix pre-existing warnings | Document separately — not your scope |
| Use deeply nested directories | Flat structure with naming prefixes |

---

## Frontmatter Reference

```yaml
---
title: "Human-readable page title"       # Required
sidebar_label: "Short Sidebar Label"     # Optional — shorter nav label
sidebar_position: 1                      # Required — 1 = first
description: "SEO and preview text"      # Recommended
---
```
