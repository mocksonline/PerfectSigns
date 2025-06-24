package su.mocks.perfectsigns.util;

import net.minecraft.util.StringIdentifiable;

/**
 * 水平对齐方式枚举
 */
public enum HorizontalAlign implements StringIdentifiable {
    LEFT("left"),
    CENTER("center"), 
    RIGHT("right");

    private final String name;

    HorizontalAlign(String name) {
        this.name = name;
    }

    @Override
    public String asString() {
        return name;
    }
} 