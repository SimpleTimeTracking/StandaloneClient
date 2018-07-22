package org.stt

import java.util.*
import java.util.function.Predicate

object Streams {

    fun <T> distinctByKey(keyExtractor: (T) -> Any): Predicate<T> {
        val visited = HashSet<Any>()
        return Predicate { item -> visited.add(keyExtractor(item)) }
    }
}
