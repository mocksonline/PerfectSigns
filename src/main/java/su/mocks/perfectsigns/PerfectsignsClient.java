package su.mocks.perfectsigns;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.MinecraftClient;
import su.mocks.perfectsigns.blockentity.PerfectSignsBlockEntities;
import su.mocks.perfectsigns.networking.EditSignPayload;
import su.mocks.perfectsigns.render.WallSignBlockEntityRenderer;
import su.mocks.perfectsigns.screen.WallSignBlockEditScreen;

@Environment(EnvType.CLIENT)
public class PerfectsignsClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        Perfectsigns.LOGGER.info("Perfect Signs 客户端正在初始化...");
        
        // 注册方块实体渲染器
        BlockEntityRendererFactories.register(PerfectSignsBlockEntities.WALL_SIGN_BLOCK_ENTITY, WallSignBlockEntityRenderer::new);
        BlockEntityRendererFactories.register(PerfectSignsBlockEntities.FULL_WALL_SIGN_BLOCK_ENTITY, WallSignBlockEntityRenderer::new);
        
        // 注册网络包处理器
        ClientPlayNetworking.registerGlobalReceiver(EditSignPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                final var world = context.client().world;
                final var player = context.client().player;
                if (world != null && player != null) {
                    final var blockEntity = world.getBlockEntity(payload.blockPos());
                    if (blockEntity instanceof su.mocks.perfectsigns.blockentity.WallSignBlockEntity entity) {
                        MinecraftClient.getInstance().setScreen(new WallSignBlockEditScreen(world.getRegistryManager(), entity, payload.blockPos()));
                    }
                }
            });
        });
        
        Perfectsigns.LOGGER.info("Perfect Signs 客户端初始化完成！");
    }
} 