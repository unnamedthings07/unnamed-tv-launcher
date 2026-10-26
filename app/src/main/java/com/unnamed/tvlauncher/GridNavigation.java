package com.unnamed.tvlauncher;

/** Pure navigation rules for the six-column TV app grid. No Android dependencies. */
final class GridNavigation {
    static final int COLUMNS = 6;

    private GridNavigation() { }

    static int clampIndex(int index, int itemCount) {
        if (itemCount <= 0) return 0;
        return Math.max(0, Math.min(index, itemCount - 1));
    }

    static int rowOf(int index) {
        return Math.max(0, index) / COLUMNS;
    }

    static int columnOf(int index) {
        return Math.max(0, index) % COLUMNS;
    }

    static int moveHorizontal(int index, int itemCount, int direction) {
        if (itemCount <= 0) return 0;
        int current = clampIndex(index, itemCount);
        int column = columnOf(current);
        if (direction < 0 && column > 0) return current - 1;
        if (direction > 0 && column < COLUMNS - 1 && current + 1 < itemCount) return current + 1;
        return current;
    }

    /** Move vertically while keeping the preferred column across incomplete final rows. */
    static int moveVertical(int index, int itemCount, int direction, int preferredColumn) {
        if (itemCount <= 0 || direction == 0) return 0;
        int current = clampIndex(index, itemCount);
        int currentRow = rowOf(current);
        int targetRow = currentRow + (direction < 0 ? -1 : 1);
        int maxRow = (itemCount - 1) / COLUMNS;
        if (targetRow < 0 || targetRow > maxRow) return current;
        int column = Math.max(0, Math.min(preferredColumn, COLUMNS - 1));
        return Math.min(targetRow * COLUMNS + column, itemCount - 1);
    }
}
