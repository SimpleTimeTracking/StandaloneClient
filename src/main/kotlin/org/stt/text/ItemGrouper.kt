package org.stt.text

import org.stt.IntRange


typealias ItemGrouper = (String) -> List<Group>

enum class Type {
    MATCH, REMAINDER
}

class Group(val type: Type, val content: String, val range: IntRange)
