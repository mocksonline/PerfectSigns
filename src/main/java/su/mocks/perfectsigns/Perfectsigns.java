package su.mocks.perfectsigns;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.mocks.perfectsigns.block.FullWallSignBlock;
import su.mocks.perfectsigns.blockentity.WallSignBlockEntity;
import su.mocks.perfectsigns.networking.EditSignPayload;
import su.mocks.perfectsigns.networking.SignEditFinishPayload;

public class Perfectsigns implements ModInitializer {
  
  // 调试开关：设为1启用调试输出，设为0禁用调试输出
  private static final int DEBUG = 0;
	public static final String MOD_ID = "perfectsigns";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// 隐形告示牌方块
	public static final FullWallSignBlock INVISIBLE_WALL_SIGN = new FullWallSignBlock(null, Block.Settings.create().mapColor(MapColor.CLEAR).noCollision().strength(0, 1f));
	public static final FullWallSignBlock INVISIBLE_GLOWING_WALL_SIGN = new FullWallSignBlock(null, Block.Settings.create().mapColor(MapColor.CLEAR).noCollision().luminance(x -> 15).strength(0, 1f));

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Perfect Signs 正在初始化...");

		// 注册网络包类型
		PayloadTypeRegistry.playC2S().register(EditSignPayload.ID, EditSignPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(EditSignPayload.ID, EditSignPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SignEditFinishPayload.ID, SignEditFinishPayload.CODEC);

		// 注册服务端网络包处理器
		ServerPlayNetworking.registerGlobalReceiver(EditSignPayload.ID, (payload, context) -> {
			context.player().server.execute(() -> {
				// 简单处理：只是确认收到了编辑请求
				var blockEntity = context.player().getWorld().getBlockEntity(payload.blockPos());
				if (blockEntity instanceof WallSignBlockEntity entity) {
					// 发送编辑界面打开请求到客户端
					ServerPlayNetworking.send(context.player(), payload);
				}
			});
		});

		// 注册编辑完成网络包处理器
		ServerPlayNetworking.registerGlobalReceiver(SignEditFinishPayload.ID, (payload, context) -> {
			context.player().server.execute(() -> {
				var blockEntity = context.player().getWorld().getBlockEntity(payload.blockPos());
				if (blockEntity instanceof WallSignBlockEntity entity) {
					if (!payload.nbt().isEmpty()) {
											if (DEBUG == 1) System.out.println("Received NBT: " + payload.nbt());
					entity.readTextsFromNbt(payload.nbt(), context.player().server.getRegistryManager());
					entity.markDirty();
					if (DEBUG == 1) System.out.println("Text contexts after reading: " + entity.textContexts.size());
						// 同步到客户端
						context.player().getWorld().updateListeners(payload.blockPos(), entity.getCachedState(), entity.getCachedState(), Block.NOTIFY_ALL);
					}
					// 清除编辑状态
					entity.setEditor(null);
				}
			});
		});

		// 注册方块
		Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "invisible_wall_sign"), INVISIBLE_WALL_SIGN);
		Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "invisible_glowing_wall_sign"), INVISIBLE_GLOWING_WALL_SIGN);

		// 注册物品
		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "invisible_wall_sign"), 
			new BlockItem(INVISIBLE_WALL_SIGN, new Item.Settings()));
		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "invisible_glowing_wall_sign"), 
			new BlockItem(INVISIBLE_GLOWING_WALL_SIGN, new Item.Settings()));

		// 注册方块实体
		su.mocks.perfectsigns.blockentity.PerfectSignsBlockEntities.register();

		// 添加到创造模式物品栏
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(content -> {
			content.add(INVISIBLE_WALL_SIGN);
			content.add(INVISIBLE_GLOWING_WALL_SIGN);
		});

		LOGGER.info("Perfect Signs 初始化完成！");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}