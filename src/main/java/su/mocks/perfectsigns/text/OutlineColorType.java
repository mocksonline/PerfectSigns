package su.mocks.perfectsigns.text;

import net.minecraft.util.StringIdentifiable;

/**
 * 描边颜色类型
 */
public enum OutlineColorType implements StringIdentifiable {
    /**
     * 无描边
     */
    NONE,
    
    /**
     * 自定义颜色描边
     */
    CUSTOM,
    
    /**
     * 告示牌样式描边（自动根据文本颜色计算）
     */
    SIGN;

  @SuppressWarnings("deprecation")
  public static final StringIdentifiable.EnumCodec<OutlineColorType> CODEC = StringIdentifiable.createCodec(OutlineColorType::values);

  private final String name;

  OutlineColorType() {
    this.name = name();
  }

  @Override
  public String asString() {
    return name;
  }

  public static OutlineColorType fromCompatibilityValue(int outlineColor) {
    return switch (outlineColor) {
      case -2 -> NONE;
      case -1 -> SIGN;
      default -> CUSTOM;
    };
  }

  public int toCompatibilityValue(int outlineColor) {
    return switch (this) {
      case NONE -> -2;
      case SIGN -> -1;
      case CUSTOM -> outlineColor;
    };
  }
} 