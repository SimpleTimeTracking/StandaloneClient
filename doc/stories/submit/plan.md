# External Submit Feature

Submit TimeTrackingItems (and their summarizations as ReportingItems) to external systems.
Triggers from both Activities view and Report view in the GUI.
Pluggable connector architecture configured via YAML.

## Requirements

1. **Pluggable connector architecture** — new submit targets added as separate implementations
2. **Global YAML config** — connectors configured once in `~/.stt/stt.yaml`
3. **GUI only** (CLI TBD)
4. **Submit status tracking** — persisted to `~/.stt/submit-status` so it survives restarts
5. **ReportListItem carries backing items** — so partial-submit fraction can be computed for summarized rows
6. **Active-connector dropdown** in both Activities and Report view toolbars — checkbox states and submit action are relative to the selected connector
7. **Initial connector**: JSON
8. **Submitted items become immutable** — once an item is submitted to any connector, it CANNOT be modified or deleted. Editing, deleting, stopping, continuing, bulk-renaming, and gap-closing are all blocked. This is enforced at two levels: (a) command handler rejects mutations, (b) UI disables action buttons.

## Architecture

```
org.stt.submit/
├── SubmitConnector.kt            # interface
├── SubmitConfig.kt               # config bean (in ConfigRoot.submit)
├── SubmitStatusTracker.kt        # per-item per-connector submit status persistence
├── SubmitSelectionManager.kt     # shared observable CheckBox state (per connector)
├── SubmitModule.kt               # Dagger bindings
└── json/
    └── JsonSubmitConnector.kt    # writes JSON file
```

### SubmitConnector interface (`SubmitConnector.kt`)

```kotlin
interface SubmitConnector {
    val id: String
    fun submitItems(items: List<TimeTrackingItem>)
    fun submitSummary(report: Report, selectedItems: List<ReportListItem>)
}
```

### Config model (`SubmitConfig.kt`)

```yaml
submit:
  connectors:
    - type: json
      file: /path/to/submit.json
```

### Active Connector

Both views hold an `ObjectProperty<SubmitConnector>` bound to a **ComboBox in the toolbar**. All checkbox and submit behavior is relative to this active connector.

Switching the dropdown recomputes every checkbox's state against the new connector. Selection sets are kept per connector, so switching away and back preserves the user's previous choices.

Runtime after config services start:
- `SubmitStatusTracker` loads `~/.stt/submit-status` into a `Map<ItemKey, Map<ConnectorId, SubmittedTimestamp>>`
- Connector instances are created from config and provided via Dagger `@IntoSet`
- `SubmitSelectionManager` holds a `Map<ConnectorId, Set<ItemKey>>` — selection state keyed by connector

### Submit Status Persistence

File: `~/.stt/submit-status` (parsed as key-value per line):

```
itemKey|connectorId|ISO-8601-timestamp
```

ItemKey = composite of `activity|start|end` (base64-encoded to avoid delimiter issues).

Methods on `SubmitStatusTracker`:
- `isSubmitted(item: TimeTrackingItem): Boolean` — submitted to ANY connector
- `isSubmitted(item: TimeTrackingItem, connectorId: String): Boolean`
- `markSubmitted(item: TimeTrackingItem, connectorId: String)`
- `getSubmitFraction(items: List<TimeTrackingItem>, connectorId: String): Float` — used by Report view for partial-indicator
- `requireNotSubmitted(item: TimeTrackingItem)` — throws if submitted (used by command handler)

### Item Locking — Command Handler Enforcement

The `Activities` class (`command/Activities.kt`) is the central command handler that all mutations pass through. It is injected with `SubmitStatusTracker`.

All mutation methods add a submit check before proceeding:

| Method | Guard | Behaviour |
|--------|-------|-----------|
| `addNewActivity` | If command replaces an existing item (`itemWithEditedActivity`), check that item is not submitted to ANY connector | Reject if submitted |
| `endCurrentActivity` | Check that ongoing item start is not submitted to ANY connector | Reject if submitted |
| `removeActivity` | Check item to delete is not submitted to ANY connector | Reject if submitted |
| `removeActivityAndCloseGap` | Check item to delete AND adjacent items are not submitted to ANY connector | Reject if any is submitted |
| `resumeActivity` | Creates new item; original unchanged — **no guard needed** | — |
| `resumeLastActivity` | Creates new item; original unchanged — **no guard needed** | — |
| `bulkChangeActivity` | Check ALL items in collection are not submitted to ANY connector | Reject if any is submitted |

### Item Locking — UI Enforcement

In `TimeTrackingItemCellWithActions`, action buttons (edit/delete/continue/stop) are disabled for items submitted to ANY connector. The disabled state is bound to `SubmitStatusTracker.isSubmitted(item)`.

In `ActivitiesController.ValidatingCommandHandler`, submit checks are added before delegating to `activities` (defence-in-depth alongside the `Activities` class enforcement).

### UI: Activities View — Checkbox Behavior

Each `TimeTrackingItemCellWithActions` gets a checkbox. Its state depends on the **active connector** (selected in the toolbar ComboBox):

| Item's submit status (for active connector) | Checkbox | User can toggle? |
|---|---|---|
| Not submitted | Unchecked | Yes |
| Already submitted | Checked, disabled | No |

- **Toolbar**: Connector ComboBox + "Submit Selected" button
- Switching the connector dropdown recomputes all checkbox states against the new connector
- Selection state (`SubmitSelectionManager`) is keyed by connector — switching away and back preserves checked items
- **"Submit Selected"** sends all checked items to the active connector via `SubmitConnector.submitItems()`, then marks them via `SubmitStatusTracker.markSubmitted()`, publishes `ItemsSubmitted` event on the bus, and calls `activityList.refresh()`
- Action buttons are disabled if the item is submitted to **any** connector (not just the active one)

### UI: Report View — Checkbox Behavior

Each `tableForReport` row gets a checkbox column. State depends on the **active connector**:

| Backing items (for active connector) | Checkbox | User can toggle? | On submit |
|---|---|---|---|
| All submitted | Checked, disabled | No | — |
| Some submitted, some not | Indeterminate, disabled | No | Submits remaining |
| None submitted | Unchecked | Yes | Submits all |

- **Toolbar**: Connector ComboBox + "Submit Selected" button
- Switching the connector dropdown recomputes all row states against the new connector
- Selection state per connector (checked rows + how many of the checked row's backing items remain to submit)
- **"Submit Selected"** sends remaining (unsubmitted) backing items for checked rows to the active connector via `SubmitConnector.submitSummary()`, then marks them, publishes `ItemsSubmitted` event on the bus, and calls `tableForReport.refresh()`

### Cross-View Sync via `ItemsSubmitted` Event

When items are submitted in one view, the other view's checkbox state becomes stale. An `ItemsSubmitted` event (`org.stt.event.ItemsSubmitted`) is published on the event bus after every successful submit. Both controllers subscribe to it:

| View | Handler | Behaviour |
|------|---------|-----------|
| Activities | `onItemsSubmitted` | Calls `activityList.refresh()` to recompute cell checkbox states |
| Report | `onItemsSubmitted` | Calls `tableForReport.refresh()` to recompute column checkbox states |

The event is published via `eventBus.publish(ItemsSubmitted())` in:
- `ActivitiesController.submitSelectedItems()` — after marking items and clearing selection
- `ReportController.submitSelectedRows()` — after marking items and clearing selection

## Implementation Order

| Step | Description | Files |
|------|-------------|-------|
| 1 | Create `SubmitConnector` interface | `submit/SubmitConnector.kt` |
| 2 | Create `SubmitConfig` + wire into `ConfigRoot` + `ConfigModule` | `submit/SubmitConfig.kt`, `ConfigRoot.kt`, `ConfigModule.kt` |
| 3 | Create `SubmitStatusTracker` with persistence (includes `isSubmitted()`, `requireNotSubmitted()`) | `submit/SubmitStatusTracker.kt` |
| 4 | Create `SubmitSelectionManager` for per-connector checkbox state | `submit/SubmitSelectionManager.kt` |
| 5 | Implement `JsonSubmitConnector` | `submit/json/JsonSubmitConnector.kt` |
| 6 | Create `SubmitModule` (Dagger bindings) | `submit/SubmitModule.kt` |
| 7 | Add `backingItems` to `ReportListItem` | `ReportController.kt` |
| 8 | **Lock submitted items in command handler** — inject `SubmitStatusTracker` into `Activities`, guard all mutation methods | `command/Activities.kt` |
| 9 | **Lock submitted items in UI** — disable action buttons in cell, add validation to `ValidatingCommandHandler` | `TimeTrackingItemCellWithActions.kt`, `ActivitiesController.kt` |
| 10 | Add connector dropdown, checkbox, and submit button to Activities view | `TimeTrackingItemCellWithActions.kt`, `ActivitiesController.kt`, `ActivitiesPanel.fxml` |
| 11 | Add connector dropdown, checkbox column, and submit button to Report view | `ReportController.kt`, `ReportPanel.fxml` |
| 12 | Register `SubmitModule` in `UIApplication` component | `UIApplication.kt` |
| 13 | **Cross-view sync** — publish `ItemsSubmitted` event after submit; both views subscribe to refresh | `event/Event.kt`, `ActivitiesController.kt`, `ReportController.kt` |

## Future Possibilities

- CSV connector
- Jira connector (reusing existing `JiraClient`)
- CLI commands (`stt submit --target json --since 7 days`)
- Submit presets (per-connector configuration in YAML, e.g., CSV delimiter, date format)
- Drag-and-drop submit to Finder/Explorer 