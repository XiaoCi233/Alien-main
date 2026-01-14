package dev.luminous.api.utils.path;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.world.BlockPosX;
import java.util.ArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class PathUtils implements Wrapper {
   public static final double range = 9.0;

   private static boolean canPassThrough(BlockPos pos) {
      Block block = mc.world.getBlockState(new BlockPos(pos.getX(), pos.getY(), pos.getZ())).getBlock();
      return block == Blocks.AIR
         || block instanceof PlantBlock
         || block == Blocks.VINE
         || block == Blocks.LADDER
         || block == Blocks.WATER
         || block == Blocks.WATER_CAULDRON
         || block instanceof WallSignBlock;
   }

   public static ArrayList<Vec3> computePath(LivingEntity fromEntity, LivingEntity toEntity) {
      return computePath(
         new Vec3(fromEntity.getX(), fromEntity.getY(), fromEntity.getZ()),
         new Vec3(toEntity.getX(), toEntity.getY(), toEntity.getZ())
      );
   }

   public static ArrayList<Vec3> computePath(Vec3d vec3d) {
      return computePath(new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ()), new Vec3(vec3d.x, vec3d.y, vec3d.z));
   }

   public static ArrayList<Vec3> computePath(Vec3 topFrom, Vec3 to) {
      if (!canPassThrough(new BlockPosX(topFrom.mc()))) {
         topFrom = topFrom.addVector(0.0, 1.0, 0.0);
      }

      AStarCustomPathFinder pathfinder = new AStarCustomPathFinder(topFrom, to);
      pathfinder.compute();
      int i = 0;
      Vec3 lastLoc = null;
      Vec3 lastDashLoc = null;
      ArrayList<Vec3> path = new ArrayList();
      ArrayList<Vec3> pathFinderPath = pathfinder.getPath();

      for (Vec3 pathElm : pathFinderPath) {
         if (i != 0 && i != pathFinderPath.size() - 1) {
            boolean canContinue = true;
            if (pathElm.squareDistanceTo(lastDashLoc) > 9.0) {
               canContinue = false;
            } else {
               double smallX = Math.min(lastDashLoc.x(), pathElm.x());
               double smallY = Math.min(lastDashLoc.y(), pathElm.y());
               double smallZ = Math.min(lastDashLoc.z(), pathElm.z());
               double bigX = Math.max(lastDashLoc.x(), pathElm.x());
               double bigY = Math.max(lastDashLoc.y(), pathElm.y());
               double bigZ = Math.max(lastDashLoc.z(), pathElm.z());

               label65:
               for (int x = (int)smallX; x <= bigX; x++) {
                  for (int y = (int)smallY; y <= bigY; y++) {
                     for (int z = (int)smallZ; z <= bigZ; z++) {
                        if (!AStarCustomPathFinder.checkPositionValidity(x, y, z, false)) {
                           canContinue = false;
                           break label65;
                        }
                     }
                  }
               }
            }

            if (!canContinue) {
               if (!path.contains(lastLoc.addVector(0.5, 0.0, 0.5))) {
                  path.add(lastLoc.addVector(0.5, 0.0, 0.5));
               }

               lastDashLoc = lastLoc;
            }
         } else {
            if (lastLoc != null && !path.contains(lastLoc.addVector(0.5, 0.0, 0.5))) {
               path.add(lastLoc.addVector(0.5, 0.0, 0.5));
            }

            if (!path.contains(pathElm.addVector(0.5, 0.0, 0.5))) {
               path.add(pathElm.addVector(0.5, 0.0, 0.5));
            }

            lastDashLoc = pathElm;
         }

         lastLoc = pathElm;
         i++;
      }

      return path;
   }
}
