package org.stt.event

import org.stt.submit.SubmitConnector


class NotifyUser(val message: String)
class ShuttingDown
class TimePassedEvent
class ItemsSubmitted

/**
 * Published when the user selects a submit connector in one view, so the
 * other view can synchronize its connector ComboBox to the same connector.
 */
class ConnectorSelected(val connector: SubmitConnector)

