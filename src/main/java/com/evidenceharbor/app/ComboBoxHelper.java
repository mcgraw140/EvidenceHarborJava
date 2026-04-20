package com.evidenceharbor.app;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import java.util.List;
import java.util.function.Function;

/**
 * Utility for making ComboBoxes type-searchable.
 */
public final class ComboBoxHelper {

    private ComboBoxHelper() {}

    /**
     * Makes a ComboBox editable and filters its items as the user types.
     *
     * @param comboBox    the combo to configure
     * @param sourceItems full list of items
     * @param labelMapper function that converts an item to display text
     */
    public static <T> void makeSearchable(ComboBox<T> comboBox, List<T> sourceItems, Function<T, String> labelMapper) {
        ObservableList<T> original = FXCollections.observableArrayList(sourceItems);
        FilteredList<T> filtered = new FilteredList<>(original, item -> true);

        comboBox.setEditable(true);
        comboBox.setItems(filtered);
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(T object) {
                return object == null ? "" : labelMapper.apply(object);
            }

            @Override
            public T fromString(String string) {
                if (string == null || string.isBlank()) return null;
                for (T item : original) {
                    if (labelMapper.apply(item).equalsIgnoreCase(string.trim())) return item;
                }
                return comboBox.getValue();
            }
        });

        comboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            String query = newText == null ? "" : newText.trim().toLowerCase();
            filtered.setPredicate(item -> labelMapper.apply(item).toLowerCase().contains(query));
            if (!comboBox.isShowing()) comboBox.show();
        });

        comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) comboBox.getEditor().setText(labelMapper.apply(newVal));
        });

        comboBox.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                String selectedText = comboBox.getValue() == null ? "" : labelMapper.apply(comboBox.getValue());
                comboBox.getEditor().setText(selectedText);
                filtered.setPredicate(item -> true);
            }
        });
    }
}
