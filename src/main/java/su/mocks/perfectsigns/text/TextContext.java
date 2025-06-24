package su.mocks.perfectsigns.text;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.mocks.perfectsigns.util.HorizontalAlign;
import su.mocks.perfectsigns.util.VerticalAlign;

/**
 * 对 Text 的简单包装与扩展，允许设置对齐属性、尺寸等参数，以便渲染时使用。
 */
public class TextContext implements Cloneable {
  
  /**
   * 文本内容
   */
  public @Nullable MutableText text;
  
  /**
   * 水平对齐方式
   */
  public HorizontalAlign horizontalAlign = HorizontalAlign.CENTER;
  
  /**
   * 垂直对齐方式
   */
  public VerticalAlign verticalAlign = VerticalAlign.MIDDLE;
  
  /**
   * 文本颜色
   */
  public int color = 0xffffff;
  
  /**
   * 是否渲染阴影
   */
  public boolean shadow = false;
  
  /**
   * X方向的偏移
   */
  public float offsetX = 0;
  
  /**
   * Y方向的偏移
   */
  public float offsetY = 0;
  
  /**
   * Z方向的偏移
   */
  public float offsetZ = 0;
  
  /**
   * 文本大小
   */
  public float size = 8;
  
  /**
   * 文本的外边框的颜色
   * -2: 无描边
   * -1: 自动描边（与文本颜色对比）
   * 其他值: 具体的颜色值
   */
  public int outlineColor = -2;
  
  /**
   * 是否粗体
   */
  public boolean bold = false;
  
  /**
   * 是否斜体
   */
  public boolean italic = false;
  
  /**
   * 是否下划线
   */
  public boolean underline = false;
  
  /**
   * 是否删除线
   */
  public boolean strikethrough = false;
  
  /**
   * 是否混淆
   */
  public boolean obfuscated = false;
  
  /**
   * 是否穿透性渲染
   */
  public boolean seeThrough = false;
  
  /**
   * X方向的旋转
   */
  public float rotationX = 0;
  
  /**
   * Y方向的旋转
   */
  public float rotationY = 0;
  
  /**
   * Z方向的旋转
   */
  public float rotationZ = 0;
  
  /**
   * X方向的缩放
   */
  public float scaleX = 1;
  
  /**
   * Y方向的缩放
   */
  public float scaleY = 1;
  
  /**
   * 是否为绝对定位
   */
  public boolean absolute = false;

  /**
   * 从一个 NBT 元素创建一个新的 TextContext 对象
   */
  public static @NotNull TextContext fromNbt(NbtElement nbt, RegistryWrapper.WrapperLookup registryLookup) {
    return fromNbt(nbt, new TextContext(), registryLookup);
  }

  /**
   * 从一个 NBT 元素创建一个新的 TextContext 对象，并使用默认值
   */
  @Contract(value = "_, _, _ -> param2", mutates = "param2")
  public static @NotNull TextContext fromNbt(NbtElement nbt, TextContext defaults, RegistryWrapper.WrapperLookup registryLookup) {
    if (nbt instanceof NbtString nbtString) {
      defaults.text = Text.literal(nbtString.asString());
    } else if (nbt instanceof NbtCompound nbtCompound) {
      defaults.readNbt(nbtCompound, registryLookup);
    }
    return defaults;
  }

  /**
   * 从 NBT 复合标签读取数据
   */
  @Contract(mutates = "this")
  public void readNbt(@NotNull NbtCompound nbt, RegistryWrapper.@Nullable WrapperLookup registryLookup) {
    if (nbt.contains("text", NbtElement.STRING_TYPE)) {
      text = Text.literal(nbt.getString("text"));
    } else if (nbt.contains("textJson", NbtElement.STRING_TYPE)) {
      try {
        text = Text.Serialization.fromJson(nbt.getString("textJson"), registryLookup);
      } catch (Exception e) {
        text = Text.literal(nbt.getString("textJson"));
      }
    }
    
    if (nbt.contains("color", NbtElement.INT_TYPE)) {
      color = nbt.getInt("color");
    }
    
    if (nbt.contains("shadow", NbtElement.BYTE_TYPE)) {
      shadow = nbt.getBoolean("shadow");
    }
    
    if (nbt.contains("offsetX", NbtElement.FLOAT_TYPE)) {
      offsetX = nbt.getFloat("offsetX");
    }
    
    if (nbt.contains("offsetY", NbtElement.FLOAT_TYPE)) {
      offsetY = nbt.getFloat("offsetY");
    }
    
    if (nbt.contains("offsetZ", NbtElement.FLOAT_TYPE)) {
      offsetZ = nbt.getFloat("offsetZ");
    }
    
    if (nbt.contains("size", NbtElement.FLOAT_TYPE)) {
      size = nbt.getFloat("size");
    }
    
    if (nbt.contains("outlineColor", NbtElement.INT_TYPE)) {
      outlineColor = nbt.getInt("outlineColor");
    }
    
    if (nbt.contains("bold", NbtElement.BYTE_TYPE)) {
      bold = nbt.getBoolean("bold");
    }
    
    if (nbt.contains("italic", NbtElement.BYTE_TYPE)) {
      italic = nbt.getBoolean("italic");
    }
    
    if (nbt.contains("underline", NbtElement.BYTE_TYPE)) {
      underline = nbt.getBoolean("underline");
    }
    
    if (nbt.contains("strikethrough", NbtElement.BYTE_TYPE)) {
      strikethrough = nbt.getBoolean("strikethrough");
    }
    
    if (nbt.contains("obfuscated", NbtElement.BYTE_TYPE)) {
      obfuscated = nbt.getBoolean("obfuscated");
    }
    
    if (nbt.contains("seeThrough", NbtElement.BYTE_TYPE)) {
      seeThrough = nbt.getBoolean("seeThrough");
    }
    
    if (nbt.contains("rotationX", NbtElement.FLOAT_TYPE)) {
      rotationX = nbt.getFloat("rotationX");
    }
    
    if (nbt.contains("rotationY", NbtElement.FLOAT_TYPE)) {
      rotationY = nbt.getFloat("rotationY");
    }
    
    if (nbt.contains("rotationZ", NbtElement.FLOAT_TYPE)) {
      rotationZ = nbt.getFloat("rotationZ");
    }
    
    if (nbt.contains("scaleX", NbtElement.FLOAT_TYPE)) {
      scaleX = nbt.getFloat("scaleX");
    }
    
    if (nbt.contains("scaleY", NbtElement.FLOAT_TYPE)) {
      scaleY = nbt.getFloat("scaleY");
    }
    
    if (nbt.contains("absolute", NbtElement.BYTE_TYPE)) {
      absolute = nbt.getBoolean("absolute");
    }
    
    if (nbt.contains("horizontalAlign", NbtElement.STRING_TYPE)) {
      String alignStr = nbt.getString("horizontalAlign");
      for (HorizontalAlign align : HorizontalAlign.values()) {
        if (align.asString().equals(alignStr)) {
          horizontalAlign = align;
          break;
        }
      }
    }
    
    if (nbt.contains("verticalAlign", NbtElement.STRING_TYPE)) {
      String alignStr = nbt.getString("verticalAlign");
      for (VerticalAlign align : VerticalAlign.values()) {
        if (align.asString().equals(alignStr)) {
          verticalAlign = align;
          break;
        }
      }
    }
  }

  /**
   * 将数据写入 NBT 复合标签
   */
  @Contract(mutates = "param1")
  public void writeNbt(@NotNull NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    if (text != null) {
      if (text.getContent() instanceof net.minecraft.text.PlainTextContent plainTextContent) {
        nbt.putString("text", plainTextContent.string());
      } else {
        nbt.putString("textJson", Text.Serialization.toJsonString(text, registryLookup));
      }
    }
    
    nbt.putInt("color", color);
    nbt.putBoolean("shadow", shadow);
    nbt.putFloat("offsetX", offsetX);
    nbt.putFloat("offsetY", offsetY);
    nbt.putFloat("offsetZ", offsetZ);
    nbt.putFloat("size", size);
    nbt.putInt("outlineColor", outlineColor);
    nbt.putBoolean("bold", bold);
    nbt.putBoolean("italic", italic);
    nbt.putBoolean("underline", underline);
    nbt.putBoolean("strikethrough", strikethrough);
    nbt.putBoolean("obfuscated", obfuscated);
    nbt.putBoolean("seeThrough", seeThrough);
    nbt.putFloat("rotationX", rotationX);
    nbt.putFloat("rotationY", rotationY);
    nbt.putFloat("rotationZ", rotationZ);
    nbt.putFloat("scaleX", scaleX);
    nbt.putFloat("scaleY", scaleY);
    nbt.putBoolean("absolute", absolute);
    nbt.putString("horizontalAlign", horizontalAlign.asString());
    nbt.putString("verticalAlign", verticalAlign.asString());
  }

  /**
   * 创建一个新的 NBT 复合标签
   */
  @Contract("_ -> new")
  public final NbtCompound createNbt(RegistryWrapper.WrapperLookup registryLookup) {
    final NbtCompound nbt = new NbtCompound();
    writeNbt(nbt, registryLookup);
    return nbt;
  }

  @Override
  public TextContext clone() {
    try {
      TextContext cloned = (TextContext) super.clone();
      if (text != null) {
        cloned.text = text.copy();
      }
      return cloned;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }

  public float getHeight() {
    return size;
  }
} 