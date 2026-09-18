package org.stt.gui.jfx

import javafx.scene.control.ComboBox
import org.stt.config.ConfigRoot
import org.stt.config.ConfigServiceFacade
import org.stt.model.TimeTrackingItem
import org.stt.submit.SubmitConnector
import org.stt.submit.SubmitStatusTracker

/**
 * Helpers for the submit connector ComboBoxes shared by the Activities and
 * Report views: restores the persisted last-used connector on startup and
 * stores the currently selected connector back into the configuration.
 */
internal fun selectLastUsedConnector(comboBox: ComboBox<SubmitConnector>, connectors: Collection<SubmitConnector>, config: ConfigRoot) {
    val storedId = config.submit.lastUsedConnectorId
    val toSelect = connectors.firstOrNull { it.id == storedId } ?: connectors.first()
    comboBox.selectionModel.select(toSelect)
}

/**
 * Persists the given connector as the last used one and saves the
 * configuration to disk immediately. Does nothing when no connector is
 * selected.
 */
internal fun persistLastUsedConnector(configService: ConfigServiceFacade, connector: SubmitConnector?) {
    if (connector != null) {
        configService.config.submit = configService.config.submit.copy(lastUsedConnectorId = connector.id)
        configService.save()
    }
}

/**
 * Selects the local connector with the given id in the combo box. Matching is
 * done by id because connector instances are not shared between views.
 */
internal fun selectConnectorById(comboBox: ComboBox<SubmitConnector>, connectors: Collection<SubmitConnector>, connectorId: String) {
    connectors.firstOrNull { it.id == connectorId }?.let { comboBox.selectionModel.select(it) }
}

/**
 * Returns true only when at least one item is present and all of the given
 * items are submitted to any connector, meaning the group is immutable.
 */
internal fun isFullySubmitted(items: List<TimeTrackingItem>, submitStatusTracker: SubmitStatusTracker): Boolean {
    return items.isNotEmpty() && items.all { submitStatusTracker.isSubmitted(it) }
}
