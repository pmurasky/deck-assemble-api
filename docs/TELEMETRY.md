# Telemetry and Privacy Contract

`config/telemetry-manifest.json` is the canonical source of truth for telemetry policy in this repository. This document is the human-readable transparency mirror for that manifest.

## Allowed runtime metadata

The baseline runtime contract is intentionally narrow. It allows only bounded metadata needed for later aggregate usage analysis:

- `tool_surface`
- `skill_id`
- `event_name`
- `timestamp`
- `sampled`
- `duration_ms`
- `outcome`
- `error_class`
- `install_id`
- `session_id`
- `repo_scope_id`
- `org_scope_id`
- `team_scope_id`

Runtime identity is tied to stable `skill_id` values from `skills/skills-index.json`. Human-readable labels such as skill names, titles, or trigger phrases are not canonical telemetry identifiers.

The initial runtime event taxonomy is also fixed and low-cardinality:

- `skill_loaded`
- `skill_invoked`
- `skill_completed`

## Forbidden data classes

The telemetry contract permanently forbids collecting these classes of data:

- `prompt_text`
- `code_content`
- `file_path`
- `argument_value`
- `secret`
- `token`
- `clipboard_content`
- `editor_content`
- `free_text`

These exclusions apply across runtime telemetry, passive adoption analytics, and any future install-registration work reserved by the contract.

## Opt-out precedence

Telemetry supports both environment and config control paths, with this precedence order:

1. `DO_NOT_TRACK`
2. `AI_CODE_SENTINEL_TELEMETRY`
3. `telemetry.mode`

Supported `telemetry.mode` values are:

- `enabled`
- `disabled`
- `log`

`log` is reserved as a transparency/debug mode for later phases so teams can inspect would-be telemetry behavior without enabling normal delivery.

Telemetry opt-out disables collection and delivery only; installs, plugin behavior, standards loading, and skill execution remain enabled.

## Passive install signal confidence

Passive install and adoption evidence is grouped by confidence:

- **High**: canonical installer-owned or adapter-owned files and markers at expected paths
- **Medium**: strong supporting install evidence that still needs corroboration
- **Low**: weak hints such as generic token or snippet references

Low-confidence signals never establish installation alone.

Concrete examples in the canonical manifest include:

- `AGENTS.md`
- `.github/copilot-instructions.md`
- `.opencode/plugins/ai-code-sentinel.js`
- `.cursor-plugin/plugin.json`
- `.claude-plugin/plugin.json`
- `gemini-extension.json`
- `installSentinel`

This keeps passive analytics honest when public GitHub visibility is sparse or incomplete.

## Adoption reporting

Phase 09 turns passive install evidence and bounded runtime telemetry into two report shapes:

- **Repo summaries** are the canonical unit.
- **Org rollups** are derived from repo summaries rather than a second schema.

Adoption reporting keeps source families and visibility states explicit so sparse public evidence is not mistaken for zero adoption.

### Reporting sources

- `passive_scan` — evidence from the passive signal inventory in `config/telemetry-manifest.json`
- `runtime_event` — bounded aggregate counts from the runtime events allowed by this contract

Passive attribution stays bounded to metadata only:

- `family`
- `marker_label`
- `source_type`
- `confidence`
- `detected`

Reports may reference canonical marker names such as `AGENTS.md` or `.opencode/plugins/ai-code-sentinel.js` for traceability, but they must never copy repository content, snippets, prompts, code, paths from observed usage, or other forbidden data classes.

### Visibility semantics

Adoption outputs must keep these states separate:

- `no_public_passive_signal` — no public passive install evidence was found
- `no_runtime_usage_observed` — no bounded runtime usage events were observed
- `unknown_private_usage_state` — visibility is insufficient to conclude zero adoption

These states are intentionally not interchangeable:

- missing public passive evidence must not erase runtime evidence
- missing runtime evidence must not imply that an install does not exist
- missing evidence in both channels defaults to explicit unknown/insufficient visibility, not a zero-adoption claim

### Interpretation guidance

- **High / medium / low confidence** still apply only to passive install evidence.
- Low-confidence passive markers never establish installation on their own.
- Runtime events show bounded observed usage, not exhaustive deployment coverage.
- Org rollups should be interpreted as aggregated visibility across repositories, with the same source and confidence caveats preserved.

## Reserved future install registration

The manifest reserves a future `install_registration` contract slot so later phases can add optional install-time registration without redesigning the schema. That slot is explicitly disabled in Phase 07 (`enabled: false`) and remains bound by the same privacy exclusions as the rest of the telemetry contract.
