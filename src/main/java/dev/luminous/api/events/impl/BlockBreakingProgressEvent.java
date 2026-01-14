package dev.luminous.api.events.impl;

import net.minecraft.util.math.BlockPos;

public class BlockBreakingProgressEvent {
   private static final BlockBreakingProgressEvent INSTANCE = new BlockBreakingProgressEvent();
   private BlockPos pos;
   private int breakerId;
   private int progress;

   private BlockBreakingProgressEvent() {
   }

   public static BlockBreakingProgressEvent get(BlockPos pos, int breakerId, int progress) {
      INSTANCE.pos = pos;
      INSTANCE.breakerId = breakerId;
      INSTANCE.progress = progress;
      return INSTANCE;
   }

   public BlockPos getPosition() {
      return this.pos;
   }

   public int getBreakerId() {
      return this.breakerId;
   }

   public int getProgress() {
      return this.progress;
   }
}
