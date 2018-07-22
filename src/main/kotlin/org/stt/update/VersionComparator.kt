package org.stt.update

import java.io.Serializable
import java.util.*

class VersionComparator : Comparator<String>, Serializable {
    override fun compare(a: String, b: String): Int {
        val aVersionParts = a.split("[.-]".toRegex()).map { this.asVersionPart(it) }
        val bVersionParts = b.split("[.-]".toRegex()).map { this.asVersionPart(it) }
        for (i in 0 until Math.min(aVersionParts.size, bVersionParts.size)) {
            if (aVersionParts[i] < bVersionParts[i]) {
                return -1
            } else if (aVersionParts[i] > bVersionParts[i]) {
                return 1
            }
        }
        return Integer.compare(aVersionParts.size, bVersionParts.size)
    }

    private fun asVersionPart(s: String): Int {
        try {
            return Integer.parseInt(s)
        } catch (e: NumberFormatException) {
            return 0
        }

    }
}
