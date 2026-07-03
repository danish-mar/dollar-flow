package com.dollarflow.ui;

/** One entry in a {@link PopupMenu}: a clickable label, or a separator line if label is null. */
public record MenuAction(String label, Runnable handler) {

    public static MenuAction separator() {
        return new MenuAction(null, null);
    }

    boolean isSeparator() {
        return label == null;
    }
}
