package org.stt.config

import java.io.File

class PathSetting(private val path: String) {

    fun path(): String {
        return path
    }

    fun file(homePath: String): File {
        return File(path.replace("\$HOME$", homePath))
    }
}
