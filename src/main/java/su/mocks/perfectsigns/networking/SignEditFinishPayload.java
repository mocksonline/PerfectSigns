package su.mocks.perfectsigns.networking;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * 编辑完成网络包
 */
public record SignEditFinishPayload(BlockPos blockPos, NbtCompound nbt) implements CustomPayload {
  public static final CustomPayload.Id<SignEditFinishPayload> ID = new CustomPayload.Id<>(Identifier.of("perfectsigns", "sign_edit_finish"));
  
      public static final PacketCodec<RegistryByteBuf, SignEditFinishPayload> CODEC = PacketCodec.of(
        (value, buf) -> {
            buf.writeBlockPos(value.blockPos);
            buf.writeNbt(value.nbt);
        },
        buf -> new SignEditFinishPayload(buf.readBlockPos(), buf.readNbt())
    );

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
} 