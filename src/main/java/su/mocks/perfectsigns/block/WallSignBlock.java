package su.mocks.perfectsigns.block;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import su.mocks.perfectsigns.blockentity.WallSignBlockEntity;
import su.mocks.perfectsigns.networking.EditSignPayload;
import su.mocks.perfectsigns.util.PerfectSignsUtils;

import java.util.Map;
import java.util.Optional;

public class WallSignBlock extends WallMountedBlock implements Waterloggable, BlockEntityProvider {
  public static final MapCodec<WallSignBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(createBaseBlockCodec(), createSettingsCodec()).apply(instance, WallSignBlock::new));
  
  protected static <B extends WallSignBlock> RecordCodecBuilder<B, Block> createBaseBlockCodec() {
    return Block.CODEC.fieldOf("base_block").forGetter(b -> b.baseBlock);
  }

  public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
  
  public static final Map<Direction, VoxelShape> SHAPES_WHEN_WALL =
      PerfectSignsUtils.createHorizontalDirectionToShape(0, 4, 0, 16, 12, 1);
  public static final Map<Direction, VoxelShape> SHAPES_WHEN_FLOOR =
      PerfectSignsUtils.createHorizontalDirectionToShape(0, 0, 4, 16, 1, 12);
  public static final Map<Direction, VoxelShape> SHAPES_WHEN_CEILING =
      PerfectSignsUtils.createHorizontalDirectionToShape(0, 15, 4, 16, 16, 12);
  
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
  
  public final Block baseBlock;

  public WallSignBlock(@Nullable Block baseBlock, Settings settings) {
    super(settings);
    this.baseBlock = baseBlock;
    setDefaultState(getDefaultState()
        .with(FACING, Direction.SOUTH)
        .with(FACE, BlockFace.WALL)
        .with(WATERLOGGED, false));
  }

  public WallSignBlock(@NotNull Block baseBlock) {
    this(baseBlock, Block.Settings.copy(baseBlock));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    super.appendProperties(builder);
    builder.add(FACE, FACING, WATERLOGGED);
  }

  @Nullable
  @Override
  public BlockState getPlacementState(ItemPlacementContext ctx) {
    final BlockState placementState = super.getPlacementState(ctx);
    return placementState != null
        ? placementState.with(
        WATERLOGGED, ctx.getWorld().getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER)
        : null;
  }

  @Override
  public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
    return true;
  }

  @Override
  public VoxelShape getOutlineShape(
      BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return SHAPE_PER_WALL_MOUNT_LOCATION.get(state.get(FACE)).get(state.get(FACING));
  }

  @Override
  public FluidState getFluidState(BlockState state) {
    return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
  }

  @Override
  public BlockState getStateForNeighborUpdate(
      BlockState state,
      Direction direction,
      BlockState neighborState,
      WorldAccess world,
      BlockPos pos,
      BlockPos neighborPos) {
    super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    if (state.get(WATERLOGGED)) {
      world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
    }
    return state;
  }

  @Override
  public MutableText getName() {
    return baseBlock == null
        ? super.getName()
        : Text.translatable("block.perfectsigns.wall_sign", baseBlock.getName());
  }

  @Override
  protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    final ActionResult actionResult = super.onUse(state, world, pos, player, hit);
    if (actionResult != ActionResult.PASS) return actionResult;
    
    // 在服务端触发打开告示牌编辑界面
    final BlockEntity blockEntity = world.getBlockEntity(pos);
    if (!(blockEntity instanceof final WallSignBlockEntity entity)) {
      return ActionResult.PASS;
    } else if (!player.getAbilities().allowModifyWorld) {
      // 冒险模式玩家无权编辑
      return ActionResult.FAIL;
    } else if (world.isClient) {
      return ActionResult.SUCCESS;
    }

    entity.checkEditorValidity();
    PlayerEntity editor = entity.getEditor();
    if (editor != null && editor != player) {
      // 告示牌被占用，玩家无权编辑
      player.sendMessage(Text.translatable("message.perfectsigns.no_editing_permission.occupied", editor.getName()), false);
      return ActionResult.FAIL;
    }
    
    // 此时告示牌已被编辑
    entity.setEditor(player);
    ServerPlayNetworking.send(((ServerPlayerEntity) player), new EditSignPayload(pos, Optional.of(hit.getSide()), Optional.empty()));
    return ActionResult.SUCCESS;
  }

  @Nullable
  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new WallSignBlockEntity(pos, state);
  }

  @Override
  protected MapCodec<? extends WallSignBlock> getCodec() {
    return CODEC;
  }
} 