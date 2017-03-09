package org.stt.gui.jfx.binding;

import javafx.beans.Observable;
import javafx.beans.binding.SetBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class MappedSetBinding<T> extends SetBinding<T> {
    private final Supplier<Set<T>> supplier;

    public MappedSetBinding(Supplier<Set<T>> supplier, Observable... sources) {
        this.supplier = requireNonNull(supplier);
        bind(Objects.requireNonNull(sources));

    }

    @Override
    protected ObservableSet<T> computeValue() {
        return FXCollections.observableSet(supplier.get());
    }
}
