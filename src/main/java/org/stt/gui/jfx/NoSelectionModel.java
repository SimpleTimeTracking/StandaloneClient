package org.stt.gui.jfx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.MultipleSelectionModel;

class NoSelectionModel<T> extends MultipleSelectionModel<T> {

	@Override
	public void clearAndSelect(int arg0) {
	}

	@Override
	public void clearSelection() {
	}

	@Override
	public void clearSelection(int arg0) {
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public boolean isSelected(int arg0) {
		return false;
	}

	@Override
	public void select(int arg0) {
	}

	@Override
	public void select(T arg0) {
	}

	@Override
	public void selectFirst() {
	}

	@Override
	public void selectLast() {
	}

	@Override
	public void selectNext() {
	}

	@Override
	public void selectPrevious() {
	}

	@Override
	public ObservableList<Integer> getSelectedIndices() {
		return FXCollections.emptyObservableList();
	}

	@Override
	public ObservableList<T> getSelectedItems() {
		return FXCollections.emptyObservableList();
	}

	@Override
	public void selectAll() {
	}

	@Override
	public void selectIndices(int arg0, int... arg1) {
	}

}
