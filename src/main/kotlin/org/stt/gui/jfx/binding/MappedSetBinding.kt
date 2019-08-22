package org.stt.gui.jfx.binding

import javafx.beans.Observable
import javafx.beans.binding.SetBinding
import javafx.collections.FXCollections
import javafx.collections.ObservableSet
import java.util.function.Supplier

class MappedSetBinding<T>(val supplier: Supplier<Set<T>>, sources: Observable) : SetBinding<T>() {

    init {
        bind(sources)
    }

    override fun computeValue(): ObservableSet<T> = FXCollections.observableSet(supplier.get())
}
