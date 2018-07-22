package org.stt.text

interface ItemCategorizer {

    enum class ItemCategory {

        BREAK, WORKTIME
    }

    fun getCategory(comment: String): ItemCategory
}
