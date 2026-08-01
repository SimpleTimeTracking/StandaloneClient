package org.stt.submit

data class SubmitConfig(
    val connectors: List<ConnectorConfig> = emptyList()
)

data class ConnectorConfig(
    val type: String = "",
    val file: String = ""
)