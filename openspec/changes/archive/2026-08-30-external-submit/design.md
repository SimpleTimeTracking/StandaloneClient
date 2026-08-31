## Context

The submit feature provides a pluggable connector architecture for submitting time tracking entries to external systems. It was built to enable users to export their time data to other tools while maintaining item immutability after submission. The feature is GUI-only (CLI is a future possibility) and is triggered from both the Activities view and the Report view.

See `proposal.md` for motivation and capability scope.

## Goals / Non-Goals

**Goals:**
- Pluggable connector interface allowing new submit targets to be added as separate implementations
- Per-item per-connector submit status persisted across restarts
- Content-based item identification (not identity-based) so items are recognized across restarts
- Two-level enforcement of item immutability after submission (command handler + UI)
- Per-connector checkbox selection state preserved when switching connectors
- Cross-view synchronization via event bus

**Non-Goals:**
- CLI submit commands (future possibility)
- CSV connector (future possibility)
- Jira connector (future possibility — existing `JiraClient` is for issue lookup, not time submission)
- Submit presets or drag-and-drop

## Decisions

### Decision 1: Content-Based Item Keying

Items are identified by a composite key `activity|start|end` Base64-encoded, not by object identity or database ID.

**Rationale:** Items are loaded fresh from the file on each restart and have no stable ID. The content-based key ensures the same logical item is recognized as submitted across restarts. The Base64 encoding avoids delimiter issues with the pipe separator used in the persistence format.

**Alternatives considered:** UUID per item would require adding and persisting a UUID field to every `TimeTrackingItem`, a much larger change.

### Decision 2: Per-Connector, Not Global, Submit Status

Submit status is tracked per connector (`Map<ItemKey, Map<ConnectorId, Timestamp>>`), not globally.

**Rationale:** A user may submit to JSON and later to Jira. The item should be locked for modification only after the first submission (to any connector), but the UI should show which connectors have already received it. The `isSubmitted(item)` check (any connector) is used for locking, while `isSubmitted(item, connectorId)` is used for per-connector checkbox display.

### Decision 3: Dagger @IntoSet for Connector Discovery

Connectors are provided via Dagger `@IntoSet` multibindings, producing a `Set<SubmitConnector>`.

**Rationale:** New connectors can be added by creating a new module with a `@IntoSet` provider — no existing code needs to change. The `SubmitModule` class only needs to be added to the Dagger component once.

### Decision 4: Two-Level Item Locking

Mutation prevention is enforced at two levels: (a) the `Activities` command handler throws `IllegalStateException`, and (b) the `TimeTrackingItemCellWithActions` UI disables action buttons.

**Rationale:** Defense in depth. The command handler is the authoritative enforcement point, but the UI layer provides immediate feedback without requiring exception handling. The `ActivitiesController.ValidatingCommandHandler` adds a third layer of defense-in-depth between the UI and the command handler.

### Decision 5: Memory-Only Selection State

`SubmitSelectionManager` stores checkbox selections in memory only, not persisted to disk.

**Rationale:** Selection is transient UI state — it makes no sense to restore checkboxes from a previous session.

### Decision 6: File Overwrite for JSON Connector

The `JsonSubmitConnector` truncates and overwrites the output file on each submit.

**Rationale:** Each submit is a snapshot of the current selection. Appending would produce duplicate entries over time and require a separate deduplication mechanism. The `submittedAt` timestamp in each record provides audit trail.

## Risks / Trade-offs

- **Risk:** Content-based key collision — two items with the same `activity|start|end` are treated as identical. **Mitigation:** This is correct behavior for the STT data model; items are uniquely identified by their content. The `start` field includes date and time, making collisions extremely unlikely.
- **Risk:** Large submit-status file over time. **Mitigation:** The file is pipe-delimited text, one line per item-connector pair. For a single user, this will remain small for years of use.
- **Risk:** JSON connector overwrites previous data. **Mitigation:** This is deliberate (snapshot model). Users who need history can use multiple output files or add a new connector implementation.
- **Trade-off:** Memory-only selection means selections are lost on restart. This is acceptable for transient UI state.

## Open Questions

None — the feature is fully implemented.