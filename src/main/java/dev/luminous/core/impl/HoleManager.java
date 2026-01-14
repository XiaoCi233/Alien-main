package dev.luminous.core.impl;

import dev.luminous.Alien;
import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.impl.combat.Breaker;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class HoleManager implements Wrapper {
   public boolean isHole(BlockPos pos) {
      return this.isHole(pos, true, false, false);
   }

   public boolean isHole(BlockPos pos, boolean canStand, boolean checkTrap, boolean anyBlock) {
      int blockProgress = 0;

      for (Direction i : Direction.values()) {
         if (i != Direction.UP && i != Direction.DOWN && (anyBlock && !mc.world.isAir(pos.offset(i)) || Alien.HOLE.isHard(pos.offset(i)))) {
            blockProgress++;
         }
      }

      return (
            !checkTrap
               || mc.world.isAir(pos)
                  && mc.world.isAir(pos.up())
                  && mc.world.isAir(pos.up(1))
                  && mc.world.isAir(pos.up(2))
                  && (mc.player.getBlockY() - 1 <= pos.getY() || mc.world.isAir(pos.up(3)))
                  && (mc.player.getBlockY() - 2 <= pos.getY() || mc.world.isAir(pos.up(4)))
         )
         && blockProgress > 3
         && (!canStand || BlockUtil.canCollide(new Box(pos.add(0, -1, 0))));
   }

   public BlockPos getHole(float range, boolean doubleHole, boolean any, boolean up) {
      BlockPos bestPos = null;
      double bestDistance = range + 1.0F;

      for (BlockPos pos : BlockUtil.getSphere(range, mc.player.getPos())) {
         if ((
               pos.getX() == mc.player.getBlockX() && pos.getZ() == mc.player.getBlockZ()
                  || up
                  || !(pos.getY() + 1 > mc.player.getY())
            )
            && (Alien.HOLE.isHole(pos, true, true, any) || doubleHole && this.isDoubleHole(pos))
            && pos.getY() - mc.player.getBlockY() <= 1) {
            double distance = MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            if (bestPos == null || distance < bestDistance) {
               bestPos = pos;
               bestDistance = distance;
            }
         }
      }

      return bestPos;
   }

   public boolean isDoubleHole(BlockPos pos) {
      Direction unHardFacing = this.is3Block(pos);
      if (unHardFacing != null) {
         pos = pos.offset(unHardFacing);
         unHardFacing = this.is3Block(pos);
         return unHardFacing != null;
      } else {
         return false;
      }
   }

   public Direction is3Block(BlockPos pos) {
      if (!this.isHard(pos.down())) {
         return null;
      } else if (mc.world.isAir(pos) && mc.world.isAir(pos.up()) && mc.world.isAir(pos.up(2))) {
         int progress = 0;
         Direction unHardFacing = null;

         for (Direction facing : Direction.values()) {
            if (facing != Direction.UP && facing != Direction.DOWN) {
               if (this.isHard(pos.offset(facing))) {
                  progress++;
               } else {
                  int progress2 = 0;

                  for (Direction facing2 : Direction.values()) {
                     if (facing2 != Direction.DOWN && facing2 != facing.getOpposite() && this.isHard(pos.offset(facing).offset(facing2))) {
                        progress2++;
                     }
                  }

                  if (progress2 == 4) {
                     progress++;
                  } else {
                     unHardFacing = facing;
                  }
               }
            }
         }

         return progress == 3 ? unHardFacing : null;
      } else {
         return null;
      }
   }

   public boolean isHard(BlockPos pos) {
      Block block = mc.world.getBlockState(pos).getBlock();
      return this.isHard(block);
   }

   public boolean isHard(Block block) {
      return block == Blocks.BEDROCK || Breaker.hard.contains(block);
   }
}
