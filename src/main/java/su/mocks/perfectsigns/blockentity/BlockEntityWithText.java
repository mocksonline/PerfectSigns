package su.mocks.perfectsigns.blockentity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import su.mocks.perfectsigns.text.TextContext;

public abstract class BlockEntityWithText extends BlockEntity {
  
  public BlockEntityWithText(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  /**
   * 获取高度
   */
  public abstract float getHeight();

  /**
   * 创建默认的文本上下文
   */
  public abstract TextContext createDefaultTextContext();

  /**
   * 获取当前编辑器
   */
  public abstract @Nullable PlayerEntity getEditor();

  /**
   * 设置编辑器
   */
  public abstract void setEditor(@Nullable PlayerEntity editor);
} 