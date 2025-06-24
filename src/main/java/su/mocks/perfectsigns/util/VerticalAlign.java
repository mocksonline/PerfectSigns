package su.mocks.perfectsigns.util;

import net.minecraft.util.StringIdentifiable;

/**
 * 垂直对齐方式枚举
 */
public enum VerticalAlign implements StringIdentifiable {
    TOP("top"),
    MIDDLE("middle"),
    BOTTOM("bottom");

    private final String name;

    VerticalAlign(String name) {
        this.name = name;
    }

    @Override
    public String asString() {
        return name;
    }
} 