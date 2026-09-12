# Item Locking Specification

## Purpose

Prevents modification or deletion of items that have been submitted to any connector, enforced at both the command handler level and the UI level.

## Requirements

### Requirement: Command Handler Guards All Mutations

The system SHALL reject mutations to submitted items in the `Activities` command handler by calling `requireNotSubmitted()` before any modification. The affected methods are: `addNewActivity`, `endCurrentActivity`, `removeActivity`, `removeActivityAndCloseGap`, and `bulkChangeActivity`.

#### Scenario: Block editing submitted item
- **WHEN** a user attempts to edit an item that has been submitted to any connector
- **THEN** the system throws an `IllegalStateException` and the item is not modified

#### Scenario: Block deleting submitted item
- **WHEN** a user attempts to delete an item that has been submitted to any connector
- **THEN** the system throws an `IllegalStateException` and the item is not deleted

#### Scenario: Block stopping submitted ongoing item
- **WHEN** a user attempts to stop an ongoing item that has been submitted to any connector
- **THEN** the system throws an `IllegalStateException` and the item is not stopped

#### Scenario: Block gap-close involving submitted item
- **WHEN** a user attempts to remove an item and close the gap, and either the target item or an adjacent item is submitted
- **THEN** the system throws an `IllegalStateException` and no items are modified

#### Scenario: Block bulk rename involving submitted items
- **WHEN** a user attempts to bulk-rename items and any item in the collection is submitted
- **THEN** the system throws an `IllegalStateException` and no items are modified

#### Scenario: Resume operations are not blocked
- **WHEN** a user resumes an activity from a submitted item
- **THEN** the operation succeeds because a new item is created and the original is unchanged

### Requirement: UI Disables Action Buttons for Submitted Items

The system SHALL disable the edit, delete, continue, and stop action buttons in `TimeTrackingItemCellWithActions` for items that are submitted to any connector.

#### Scenario: Action buttons disabled
- **WHEN** an item is submitted to any connector
- **THEN** the edit, delete, continue, and stop buttons are disabled for that item in the Activities view

#### Scenario: Action buttons enabled for unsubmitted items
- **WHEN** an item is not submitted to any connector
- **THEN** all action buttons are enabled for that item