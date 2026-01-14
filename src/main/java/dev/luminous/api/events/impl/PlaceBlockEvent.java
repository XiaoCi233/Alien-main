package dev.luminous.api.events.impl;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public class PlaceBlockEvent {
   private static final PlaceBlockEvent INSTANCE = new PlaceBlockEvent();
   public BlockPos blockPos;
   public Block block;

   private PlaceBlockEvent() {
   }

   public static PlaceBlockEvent get(BlockPos blockPos, Block block) {
      INSTANCE.blockPos = blockPos;
      INSTANCE.block = block;
      return INSTANCE;
   }
}
