package org.levimc.launcher.core.mods.memoryeditor;

public enum SearchCondition {
    EQUALS(0, "="),
    NOT_EQUALS(1, "≠"),
    GREATER(2, ">"),
    LESS(3, "<"),
    GREATER_EQUALS(4, "≥"),
    LESS_EQUALS(5, "≤");

    private final int id;
    private final String symbol;

    SearchCondition(int id, String symbol) {
        this.id = id;
        this.symbol = symbol;
    }

    public int getId() { return id; }
    public String getSymbol() { return symbol; }

    public static SearchCondition fromId(int id) {
        for (SearchCondition c : values()) {
            if (c.id == id) return c;
        }
        return EQUALS;
    }
}
