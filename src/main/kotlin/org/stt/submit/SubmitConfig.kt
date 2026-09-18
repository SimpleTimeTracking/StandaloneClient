package org.stt.submit

data class SubmitConfig(
    val connectors: List<ConnectorConfig> = mutableListOf(),
    val lastUsedConnectorId: String? = null
)

data class ConnectorConfig(
    val type: String = "",
    val file: String = ""
)