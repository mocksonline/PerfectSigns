package su.mocks.perfectsigns.util;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.DataResult;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import su.mocks.perfectsigns.text.TextContext;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Map;

public class PerfectSignsUtils {
  
  // 调试开关：设为1启用调试输出，设为0禁用调试输出
  private static final int DEBUG = 0;
    
    /**
     * 创建水平方向到形状的映射
     */
    public static Map<Direction, VoxelShape> createHorizontalDirectionToShape(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        final VoxelShape southShape = VoxelShapes.cuboid(minX / 16, minY / 16, minZ / 16, maxX / 16, maxY / 16, maxZ / 16);
        final VoxelShape westShape = VoxelShapes.cuboid(1 - maxZ / 16, minY / 16, minX / 16, 1 - minZ / 16, maxY / 16, maxX / 16);
        final VoxelShape northShape = VoxelShapes.cuboid(1 - maxX / 16, minY / 16, 1 - maxZ / 16, 1 - minX / 16, maxY / 16, 1 - minZ / 16);
        final VoxelShape eastShape = VoxelShapes.cuboid(minZ / 16, minY / 16, 1 - maxX / 16, maxZ / 16, maxY / 16, 1 - minX / 16);
        
        return ImmutableMap.of(
            Direction.SOUTH, southShape,
            Direction.WEST, westShape,
            Direction.NORTH, northShape,
            Direction.EAST, eastShape
        );
    }
    
    /**
     * 将颜色转化为告示牌发光的颜色。若为黑色，则返回白色。
     */
      public static int toSignOutlineColor(int color) {
    if ((color & 0xffffff) == 0) {
      return (color & 0xff000000) | 0xf0ebcc;
    }
    int j = (int) ((double) ColorHelper.Argb.getRed(color) * 0.4);
    int k = (int) ((double) ColorHelper.Argb.getGreen(color) * 0.4);
    int l = (int) ((double) ColorHelper.Argb.getBlue(color) * 0.4);
    return ColorHelper.Argb.getArgb(ColorHelper.Argb.getAlpha(color), j, k, l);
  }

  /**
   * 根据 signColor 返回颜色值。
   *
   * @param signColor 对应的告示牌颜色（整数）。
   * @return 颜色枚举对象，可能为 null。
   */
  public static DyeColor colorBySignColor(int signColor) {
    for (DyeColor color : DyeColor.values()) {
      if (color.getSignColor() == signColor) {
        return color;
      }
    }
    return null;
  }
    
    public static MutableText describeColor(int color) {
        return Text.literal(formatColorHex(color)).styled(style -> style.withColor(color));
    }
    
    public static MutableText describeColor(int color, Text text) {
        return Text.literal("").append(text).styled(style -> style.withColor(color));
    }
    
    public static String formatColorHex(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }
    
    public static DataResult<Integer> parseColor(String s) {
        if (DEBUG == 1) System.out.println("parseColor called with: " + s);
        if (s.startsWith("#")) {
            try {
                // 移除#前缀并解析十六进制
                String hex = s.substring(1);
                if (hex.length() == 6) {
                    int result = Integer.parseInt(hex, 16);
                    // 确保颜色值包含完整的RGB信息
                    result = result | 0xFF000000; // 添加alpha通道
                    return DataResult.success(result);
                } else if (hex.length() == 3) {
                    // 支持简短格式如#F0F -> #FF00FF
                    char r = hex.charAt(0);
                    char g = hex.charAt(1);
                    char b = hex.charAt(2);
                    String fullHex = "" + r + r + g + g + b + b;
                    int result = Integer.parseInt(fullHex, 16);
                    // 确保颜色值包含完整的RGB信息
                    result = result | 0xFF000000; // 添加alpha通道
                    return DataResult.success(result);
                } else {
                    return DataResult.error(() -> "Invalid hex color length: " + s);
                }
            } catch (NumberFormatException e) {
                return DataResult.error(() -> "Invalid hex color: " + s);
            }
        }
        
        // 特殊值不在这里处理，由调用者直接处理
        
        // 尝试解析颜色名称
        for (Formatting formatting : Formatting.values()) {
            if (formatting.isColor() && formatting.getName().equalsIgnoreCase(s)) {
                Integer color = formatting.getColorValue();
                if (color != null) {
                    return DataResult.success(color);
                }
            }
        }
        
        // 尝试解析十进制数字
        try {
            int result = Integer.parseInt(s);
            return DataResult.success(result);
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Invalid color: " + s);
        }
    }
    
    public static MutableText describeShortcut(Text shortcut) {
        return Text.literal("(").append(shortcut).append(")").styled(style -> style.withColor(0x888888));
    }
    
    public static void rearrange(Collection<TextContext> textContexts) {
        // 重新排列文本上下文的逻辑
        // 这里可以根据需要实现具体的排列逻辑
    }
    
    public static String numberToString(float number) {
        if (number == (int) number) {
            return String.valueOf((int) number);
        }
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(number);
    }
} 