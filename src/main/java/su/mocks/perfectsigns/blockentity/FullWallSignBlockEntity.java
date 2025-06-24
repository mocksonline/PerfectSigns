package su.mocks.perfectsigns.blockentity;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import su.mocks.perfectsigns.text.TextContext;

public class FullWallSignBlockEntity extends WallSignBlockEntity {
  public FullWallSignBlockEntity(BlockPos pos, BlockState state) {
    super(PerfectSignsBlockEntities.FULL_WALL_SIGN_BLOCK_ENTITY, pos, state);
  }

  @Override
  public float getHeight() {
    return 16;
  }

  @Override
  public TextContext createDefaultTextContext() {
    final TextContext textContext = new TextContext();
    textContext.size = 8;
    return textContext;
  }
} 