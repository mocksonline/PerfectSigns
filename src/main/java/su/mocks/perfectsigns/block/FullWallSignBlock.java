package su.mocks.perfectsigns.block;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.TransparentBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import su.mocks.perfectsigns.blockentity.FullWallSignBlockEntity;
import su.mocks.perfectsigns.util.PerfectSignsUtils;

import java.util.Map;

public class FullWallSignBlock extends WallSignBlock {
  public static final MapCodec<FullWallSignBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(createBaseBlockCodec(), createSettingsCodec()).apply(instance, FullWallSignBlock::new));

  public static final Map<Direction, VoxelShape> SHAPES_WHEN_WALL =
      PerfectSignsUtils.createHorizontalDirectionToShape(0, 0, 0, 16, 16, 1);
  public static final Map<Direction, VoxelShape> SHAPES_WHEN_FLOOR =
      PerfectSignsUtils.createHorizontalDirectionToShape(0, 0, 0, 16, 1, 16);
  public static final Map<Direction, VoxelShape> SHAPES_WHEN_CEILING =
      PerfectSignsUtils.createHorizontalDirectionToShape(0, 15, 0, 16, 16, 16);

  @Unmodifiable
  public static final Map<BlockFace, Map<Direction, VoxelShape>>
      SHAPE_PER_WALL_MOUNT_LOCATION =
      ImmutableMap.of(
          BlockFace.CEILING,
          SHAPES_WHEN_CEILING,
          BlockFace.FLOOR,
          SHAPES_WHEN_FLOOR,
          BlockFace.WALL,
          SHAPES_WHEN_WALL);

  public FullWallSignBlock(@Nullable Block baseBlock, Settings settings) {
    super(baseBlock, settings);
  }

  public FullWallSignBlock(@NotNull Block baseBlock) {
    this(baseBlock, Block.Settings.copy(baseBlock));
  }

  @Override
  public MutableText getName() {
    return baseBlock == null
        ? super.getName()
        : Text.translatable("block.perfectsigns.full_wall_sign", baseBlock.getName());
  }

  @Override
  public VoxelShape getOutlineShape(
      BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return SHAPE_PER_WALL_MOUNT_LOCATION.get(state.get(FACE)).get(state.get(FACING));
  }

  @Override
  public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new FullWallSignBlockEntity(pos, state);
  }

  @Override
  public boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
    if (direction.getAxis().isHorizontal() && state.getBlock() instanceof FullWallSignBlock && stateFrom.getBlock() instanceof FullWallSignBlock wallSignBlockFrom && state.get(FACING) == stateFrom.get(FACING) && direction.getAxis() != state.get(FACING).getAxis()) {
      if (wallSignBlockFrom.baseBlock instanceof TransparentBlock) {
        if (baseBlock instanceof TransparentBlock) {
          // 自身和相邻方块都为透明方块，则双方均为同一方块时隐藏。
          return baseBlock == wallSignBlockFrom.baseBlock;
        } else {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }

  @Override
  protected MapCodec<? extends FullWallSignBlock> getCodec() {
    return CODEC;
  }
} 