package su.mocks.perfectsigns.blockentity;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import su.mocks.perfectsigns.Perfectsigns;

public class PerfectSignsBlockEntities {
  
  public static final BlockEntityType<WallSignBlockEntity> WALL_SIGN_BLOCK_ENTITY = 
    BlockEntityType.Builder.create(WallSignBlockEntity::new, 
      Perfectsigns.INVISIBLE_WALL_SIGN, Perfectsigns.INVISIBLE_GLOWING_WALL_SIGN)
      .build();
      
  public static final BlockEntityType<FullWallSignBlockEntity> FULL_WALL_SIGN_BLOCK_ENTITY = 
    BlockEntityType.Builder.create(FullWallSignBlockEntity::new, 
      Perfectsigns.INVISIBLE_WALL_SIGN, Perfectsigns.INVISIBLE_GLOWING_WALL_SIGN)
      .build();

  public static void register() {
    Registry.register(Registries.BLOCK_ENTITY_TYPE, 
      Perfectsigns.id("wall_sign"), WALL_SIGN_BLOCK_ENTITY);
    Registry.register(Registries.BLOCK_ENTITY_TYPE, 
      Perfectsigns.id("full_wall_sign"), FULL_WALL_SIGN_BLOCK_ENTITY);
  }
} 