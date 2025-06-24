package su.mocks.perfectsigns.blockentity;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import su.mocks.perfectsigns.text.TextContext;

import java.util.List;

public class WallSignBlockEntity extends BlockEntityWithText {
  public static final TextContext DEFAULT_TEXT_CONTEXT = new TextContext();
  static {
    DEFAULT_TEXT_CONTEXT.size = 6;
  }
  
  /**
   * 正在编辑该告示牌的玩家。若为 null，则表示该告示牌为空闲模式。
   */
  public @Nullable PlayerEntity editor;

  @NotNull
  public @Unmodifiable List<TextContext> textContexts = ImmutableList.of();
  
  /**
   * 告示牌的文本是否正在发光，不影响文本的颜色和描边，只影响文本显示时的所使用的亮度。
   */
  public boolean glowing;
  
  /**
   * 告示牌是否已经被涂蜡。
   */
  public boolean waxed;

  public WallSignBlockEntity(BlockPos pos, BlockState state) {
    super(PerfectSignsBlockEntities.WALL_SIGN_BLOCK_ENTITY, pos, state);
  }

  protected WallSignBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    super.readNbt(nbt, registryLookup);
    final @Nullable NbtElement nbtText = nbt.get("text");
    if (nbtText instanceof NbtString || nbt.contains("textJson", NbtElement.STRING_TYPE)) {
      // 如果 text 是个字符串，则读取整个 nbt 作为 TextContext。
      textContexts = ImmutableList.of(TextContext.fromNbt(nbt, createDefaultTextContext(), registryLookup));
    } else if (nbtText instanceof NbtCompound) {
      // 如果 text 是个复合标签，则读取这个复合标签。
      textContexts = ImmutableList.of(TextContext.fromNbt(nbtText, createDefaultTextContext(), registryLookup));
    } else if (nbtText instanceof NbtList) {
      ImmutableList.Builder<TextContext> builder = new ImmutableList.Builder<>();
      for (NbtElement nbtElement : ((NbtList) nbtText)) {
        builder.add(TextContext.fromNbt(nbtElement, createDefaultTextContext(), registryLookup));
      }
      textContexts = builder.build();
    } else {
      // 如果没有文本数据，创建空列表
      textContexts = ImmutableList.of();
    }

    glowing = nbt.getBoolean("glowing");
    waxed = nbt.getBoolean("waxed");
  }

  @Override
  public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    super.writeNbt(nbt, registryLookup);
    if (textContexts.size() == 1) {
      final NbtCompound nbtCompound = new NbtCompound();
      textContexts.get(0).writeNbt(nbtCompound, registryLookup);
      nbt.put("text", nbtCompound);
    } else {
      final NbtList nbtList = new NbtList();
      for (TextContext textContext : textContexts) {
        nbtList.add(textContext.createNbt(registryLookup));
      }
      nbt.put("text", nbtList);
    }
    nbt.putBoolean("glowing", glowing);
    nbt.putBoolean("waxed", waxed);
  }

  @Override
  public float getHeight() {
    return 8;
  }

  @Override
  public TextContext createDefaultTextContext() {
    return DEFAULT_TEXT_CONTEXT.clone();
  }

  @Override
  public @Nullable PlayerEntity getEditor() {
    return editor;
  }

  @Override
  public void setEditor(@Nullable PlayerEntity editor) {
    this.editor = editor;
  }

  public void checkEditorValidity() {
    if (editor != null && !editor.isAlive()) {
      editor = null;
    }
  }

  /**
   * 从NBT读取文本数据
   */
  public void readTextsFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    final @Nullable NbtElement nbtText = nbt.get("text");
    if (nbtText instanceof NbtString || nbt.contains("textJson", NbtElement.STRING_TYPE)) {
      // 如果 text 是个字符串，则读取整个 nbt 作为 TextContext。
      textContexts = ImmutableList.of(TextContext.fromNbt(nbt, createDefaultTextContext(), registryLookup));
    } else if (nbtText instanceof NbtCompound) {
      // 如果 text 是个复合标签，则读取这个复合标签。
      textContexts = ImmutableList.of(TextContext.fromNbt(nbtText, createDefaultTextContext(), registryLookup));
    } else if (nbtText instanceof NbtList) {
      ImmutableList.Builder<TextContext> builder = new ImmutableList.Builder<>();
      for (NbtElement nbtElement : ((NbtList) nbtText)) {
        builder.add(TextContext.fromNbt(nbtElement, createDefaultTextContext(), registryLookup));
      }
      textContexts = builder.build();
    } else {
      // 如果没有文本数据，创建空列表
      textContexts = ImmutableList.of();
    }
  }

  /**
   * 获取用于客户端同步的更新包
   */
  @Override
  public BlockEntityUpdateS2CPacket toUpdatePacket() {
    return BlockEntityUpdateS2CPacket.create(this);
  }

  /**
   * 获取用于初始区块数据的NBT
   */
  @Override
  public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
    return createNbt(registryLookup);
  }
} 