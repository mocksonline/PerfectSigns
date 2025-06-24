package su.mocks.perfectsigns.render;

import com.google.common.collect.ImmutableSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.Unmodifiable;
import su.mocks.perfectsigns.Perfectsigns;
import su.mocks.perfectsigns.block.WallSignBlock;
import su.mocks.perfectsigns.blockentity.WallSignBlockEntity;

import su.mocks.perfectsigns.text.TextContext;
import su.mocks.perfectsigns.util.HorizontalAlign;
import su.mocks.perfectsigns.util.PerfectSignsUtils;
import su.mocks.perfectsigns.util.VerticalAlign;

import java.util.Collection;

@Environment(EnvType.CLIENT)
public class WallSignBlockEntityRenderer<T extends WallSignBlockEntity> implements BlockEntityRenderer<T> {

  /**
   * 这个集合中的方块，在渲染时是视为没有厚度的，直接渲染在靠墙的位置
   */
  private static final @Unmodifiable Collection<Block> INVISIBLE_BLOCKS =
      ImmutableSet.of(Perfectsigns.INVISIBLE_WALL_SIGN, Perfectsigns.INVISIBLE_GLOWING_WALL_SIGN);

  public WallSignBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    // 构造函数参数是必需的，但我们不需要存储ctx
  }

  @Override
  public void render(T entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
    final Block block = entity.getCachedState().getBlock();
    
    // 若方块为隐形方块，且玩家手中拿着该方块，则显示该方块轮廓
    final ClientPlayerEntity player = MinecraftClient.getInstance().player;
    if (INVISIBLE_BLOCKS.contains(block) && player != null) {
      final Item mainHandStackItem = player.getMainHandStack().getItem();
      if (mainHandStackItem instanceof final BlockItem blockItem
          && INVISIBLE_BLOCKS.contains(blockItem.getBlock())) {
        renderOutline(entity, matrices, vertexConsumers, light, overlay);
      }
    }

    // 渲染文本
    renderTexts(entity, tickDelta, matrices, vertexConsumers, light, overlay);
  }

  private void renderOutline(T entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
    // 渲染隐形方块的轮廓，当玩家手持对应方块时显示
    BlockState state = entity.getCachedState();
    VoxelShape shape = state.getOutlineShape(entity.getWorld(), entity.getPos());
    
    if (!shape.isEmpty()) {
      matrices.push();
      
      VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
      WorldRenderer.drawShapeOutline(matrices, vertexConsumer, shape, 0.0, 0.0, 0.0, 1.0f, 1.0f, 1.0f, 0.4f, false);
      
      matrices.pop();
    }
  }

  private void renderTexts(T entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
    if (entity.textContexts.isEmpty()) {
      return;
    }

    matrices.push();
    
    // 设置基础变换
    matrices.translate(0.5, 0.5, 0.5);
    final BlockState state = entity.getCachedState();
    final Direction facing = state.get(WallSignBlock.FACING);
    final BlockFace face = state.get(WallSignBlock.FACE);
    
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation()));
    matrices.multiply(
        RotationAxis.POSITIVE_X.rotationDegrees(
            face == BlockFace.CEILING ? 90 : face == BlockFace.FLOOR ? -90 : 0));
    if (face != BlockFace.WALL) {
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));
    }
    
    matrices.scale(1 / 16f, -1 / 16f, 1 / 16f);
    matrices.translate(0, 0, (INVISIBLE_BLOCKS.contains(entity.getCachedState().getBlock()) ? -8 : -7) + .0125);

    // 如果告示牌发光，使用最大光照
    if (entity.glowing) {
      light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
    }

    final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

    // 渲染每个文本上下文
    for (TextContext textContext : entity.textContexts) {
      if (textContext.text == null) continue;
      
      renderSingleText(textContext, matrices, vertexConsumers, textRenderer, light, overlay);
    }
    
    matrices.pop();
  }

  private void renderSingleText(TextContext textContext, MatrixStack matrices, VertexConsumerProvider vertexConsumers, TextRenderer textRenderer, int light, int overlay) {
    matrices.push();
    
    // 应用文本变换
    applyTextTransform(textContext, matrices);
    
    // 获取文本内容
    Text text = textContext.text;
    if (text == null) {
      matrices.pop();
      return;
    }
    
    // 应用文本样式
    Style style = text.getStyle()
        .withBold(textContext.bold)
        .withItalic(textContext.italic)
        .withUnderline(textContext.underline)
        .withStrikethrough(textContext.strikethrough)
        .withObfuscated(textContext.obfuscated)
        .withColor(textContext.color);
    
    Text styledText = text.copy().setStyle(style);
    OrderedText orderedText = styledText.asOrderedText();
    
    // 计算文本位置
    float textWidth = textRenderer.getWidth(orderedText);
    float textHeight = textRenderer.fontHeight;
    
    float x = calculateHorizontalOffset(textContext.horizontalAlign, textWidth);
    float y = calculateVerticalOffset(textContext.verticalAlign, textHeight);
    
    // 渲染文本（包含描边处理）
    renderTextWithOutline(orderedText, x, y, textContext, matrices, vertexConsumers, textRenderer, light);
    
    matrices.pop();
  }

  private void applyTextTransform(TextContext textContext, MatrixStack matrices) {
    // 应用偏移
    matrices.translate(textContext.offsetX, textContext.offsetY, textContext.offsetZ);
    
    // 应用旋转
    if (textContext.rotationX != 0) {
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(textContext.rotationX));
    }
    if (textContext.rotationY != 0) {
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(textContext.rotationY));
    }
    if (textContext.rotationZ != 0) {
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(textContext.rotationZ));
    }
    
    // 应用缩放
    matrices.scale(textContext.scaleX * textContext.size / 16f, textContext.scaleY * textContext.size / 16f, 1.0f);
  }

  private float calculateHorizontalOffset(HorizontalAlign align, float textWidth) {
    return switch (align) {
      case LEFT -> 0f;                // 左对齐：不偏移（文本从左边开始）
      case CENTER -> -textWidth / 2f; // 居中：向左偏移半个文本宽度
      case RIGHT -> -textWidth;       // 右对齐：向左偏移整个文本宽度
    };
  }

  private float calculateVerticalOffset(VerticalAlign align, float textHeight) {
    return switch (align) {
      case TOP -> 0f;                 // 顶部对齐：不偏移（文本从顶部开始）
      case MIDDLE -> -textHeight / 2f; // 居中：向上偏移半个文本高度
      case BOTTOM -> -textHeight;     // 底部对齐：向上偏移整个文本高度
    };
  }

  private void renderTextWithOutline(OrderedText text, float x, float y, TextContext textContext, MatrixStack matrices, VertexConsumerProvider vertexConsumers, TextRenderer textRenderer, int light) {
    if (textContext.outlineColor == -2) {
      // 无描边，直接渲染文本
      textRenderer.draw(
          text,
          x,
          y,
          textContext.color,
          textContext.shadow,
          matrices.peek().getPositionMatrix(),
          vertexConsumers,
          textContext.seeThrough ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL,
          0,
          light
      );
    } else {
      // 有描边，使用原生的drawWithOutline方法
      int outlineColor = textContext.outlineColor;
      if (textContext.outlineColor == -1) { // -1 表示自动描边
        outlineColor = PerfectSignsUtils.toSignOutlineColor(textContext.color);
      }
      
      textRenderer.drawWithOutline(
          text,
          x,
          y,
          textContext.color,
          outlineColor,
          matrices.peek().getPositionMatrix(),
          vertexConsumers,
          light
      );
    }
  }
} 