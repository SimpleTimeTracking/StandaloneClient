package org.stt.gui.jfx.binding;

import javafx.beans.Observable;
import javafx.beans.binding.ListBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class MappedListBinding<T> extends ListBinding<T> {
    private final Supplier<List<T>> mapper;

    public MappedListBinding(Supplier<List<T>> mapper, Observable... source) {
        this.mapper = requireNonNull(mapper);
        bind(Objects.requireNonNull(source));
    }

    @Override
    protected ObservableList<T> computeValue() {
        return FXCollections.observableList(mapper.get());
    }
}
