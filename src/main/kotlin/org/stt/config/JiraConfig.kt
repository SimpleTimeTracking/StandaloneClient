package org.stt.config

class JiraConfig : ConfigurationContainer {
    var jiraURI: String? = null
    var jiraUsername: String? = null
    var jiraPassword: PasswordSetting? = null
}
