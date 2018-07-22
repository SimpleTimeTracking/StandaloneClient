package org.stt.text

@FunctionalInterface
interface ExpansionProvider {
    fun getPossibleExpansions(text: String): List<String>
}
