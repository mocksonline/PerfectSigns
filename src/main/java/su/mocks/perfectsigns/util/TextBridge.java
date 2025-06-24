package su.mocks.perfectsigns.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * 文本桥接类，提供创建文本的便捷方法
 */
public class TextBridge {
    
    public static MutableText literal(String string) {
        return Text.literal(string);
    }
    
    public static MutableText translatable(String key) {
        return Text.translatable(key);
    }
    
    public static MutableText translatable(String key, Object... args) {
        return Text.translatable(key, args);
    }
    
    public static MutableText empty() {
        return Text.empty();
    }
} 