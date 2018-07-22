package org.stt

object Strings {

    fun commonPrefix(a: String, b: String): String {
        var i = 0
        while (i < a.length && i < b.length) {
            if (a[i] != b[i]) {
                return a.substring(0, i)
            }
            i++
        }
        return a
    }
}
