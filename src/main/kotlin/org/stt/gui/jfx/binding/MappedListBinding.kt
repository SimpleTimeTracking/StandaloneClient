package org.stt.gui.jfx.binding

import javafx.beans.Observable
import javafx.beans.binding.ListBinding
import javafx.collections.FXCollections
import javafx.collections.ObservableList

class MappedListBinding<T>(val mapper: () -> List<T>, source: Observable) : ListBinding<T>() {

    init {
        bind(source)
    }

    override fun computeValue(): ObservableList<T> = FXCollections.observableList(mapper())
}
