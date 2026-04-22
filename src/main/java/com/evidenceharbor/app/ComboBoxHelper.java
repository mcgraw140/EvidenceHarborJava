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

        // Track whether edits are user-driven (typing) vs programmatic (setValue / selection).
        final boolean[] suppressFilter = { false };

        comboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (suppressFilter[0]) return;
            if (!comboBox.getEditor().isFocused()) return; // ignore programmatic text changes
            String query = newText == null ? "" : newText.trim().toLowerCase();
            filtered.setPredicate(item -> labelMapper.apply(item).toLowerCase().contains(query));

            // Keep value in sync: if editor text exactly matches an item, update the selection.
            T match = null;
            for (T item : original) {
                if (labelMapper.apply(item).equalsIgnoreCase(query.trim())) { match = item; break; }
            }
            if (match != null && match != comboBox.getValue()) {
                suppressFilter[0] = true;
                try { comboBox.setValue(match); } finally { suppressFilter[0] = false; }
            } else if (match == null && comboBox.getValue() != null) {
                // Text no longer matches the selected value - clear selection so OK validation catches it.
                suppressFilter[0] = true;
                try { comboBox.setValue(null); } finally { suppressFilter[0] = false; }
            }

            if (!comboBox.isShowing() && comboBox.getEditor().isFocused()) comboBox.show();
        });

        comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            suppressFilter[0] = true;
            try {
                comboBox.getEditor().setText(newVal == null ? "" : labelMapper.apply(newVal));
            } finally { suppressFilter[0] = false; }
        });

        comboBox.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                // On blur: if editor text matches an item, keep the match; otherwise snap back.
                String typed = comboBox.getEditor().getText();
                T match = null;
                if (typed != null && !typed.isBlank()) {
                    for (T item : original) {
                        if (labelMapper.apply(item).equalsIgnoreCase(typed.trim())) { match = item; break; }
                    }
                }
                suppressFilter[0] = true;
                try {
                    if (match != null) {
                        comboBox.setValue(match);
                    } else {
                        comboBox.getEditor().setText(comboBox.getValue() == null ? "" : labelMapper.apply(comboBox.getValue()));
                    }
                    filtered.setPredicate(item -> true);
                } finally { suppressFilter[0] = false; }
            }
        });
    }
}
