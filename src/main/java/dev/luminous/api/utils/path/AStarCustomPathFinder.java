package dev.luminous.api.utils.path;

import dev.luminous.api.utils.Wrapper;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.block.CactusBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PistonExtensionBlock;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.util.math.BlockPos;

public class AStarCustomPathFinder implements Wrapper {
   private static final Vec3[] flatCardinalDirections = new Vec3[]{
      new Vec3(1.0, 0.0, 0.0), new Vec3(-1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), new Vec3(0.0, 0.0, -1.0)
   };
   private final Vec3 startVec3;
   private final Vec3 endVec3;
   private final ArrayList<AStarCustomPathFinder.Hub> hubs = new ArrayList();
   private final ArrayList<AStarCustomPathFinder.Hub> hubsToWork = new ArrayList();
   private ArrayList<Vec3> path = new ArrayList();

   public AStarCustomPathFinder(Vec3 startVec3, Vec3 endVec3) {
      this.startVec3 = startVec3.addVector(0.0, 0.0, 0.0).floor();
      this.endVec3 = endVec3.addVector(0.0, 0.0, 0.0).floor();
   }

   public static boolean checkPositionValidity(Vec3 loc, boolean checkGround) {
      return checkPositionValidity((int)loc.x(), (int)loc.y(), (int)loc.z(), checkGround);
   }

   public static boolean checkPositionValidity(int x, int y, int z, boolean checkGround) {
      BlockPos block1 = new BlockPos(x, y, z);
      BlockPos block2 = new BlockPos(x, y + 1, z);
      BlockPos block3 = new BlockPos(x, y - 1, z);
      return !isBlockSolid(block1) && !isBlockSolid(block2) && (isBlockSolid(block3) || !checkGround) && isSafeToWalkOn(block3);
   }

   private static boolean isBlockSolid(BlockPos block) {
      return mc.world.getBlockState(block).shapeCache != null && mc.world.getBlockState(block).shapeCache.isFullCube
         || mc.world.getBlockState(block).getBlock() instanceof SlabBlock
         || mc.world.getBlockState(block).getBlock() instanceof StairsBlock
         || mc.world.getBlockState(block).getBlock() instanceof CactusBlock
         || mc.world.getBlockState(block).getBlock() instanceof ChestBlock
         || mc.world.getBlockState(block).getBlock() instanceof EnderChestBlock
         || mc.world.getBlockState(block).getBlock() instanceof SkullBlock
         || mc.world.getBlockState(block).getBlock() instanceof PaneBlock
         || mc.world.getBlockState(block).getBlock() instanceof FenceBlock
         || mc.world.getBlockState(block).getBlock() instanceof WallBlock
         || mc.world.getBlockState(block).getBlock() instanceof StainedGlassBlock
         || mc.world.getBlockState(block).getBlock() instanceof PistonBlock
         || mc.world.getBlockState(block).getBlock() instanceof PistonExtensionBlock
         || mc.world.getBlockState(block).getBlock() instanceof PistonHeadBlock
         || mc.world.getBlockState(block).getBlock() instanceof StainedGlassBlock
         || mc.world.getBlockState(block).getBlock() instanceof TrapdoorBlock;
   }

   private static boolean isSafeToWalkOn(BlockPos block) {
      return !(mc.world.getBlockState(block).getBlock() instanceof FenceBlock) && !(mc.world.getBlockState(block).getBlock() instanceof WallBlock);
   }

   public ArrayList<Vec3> getPath() {
      return this.path;
   }

   public void compute() {
      this.compute(1000, 4);
   }

   public void compute(int loops, int depth) {
      this.path.clear();
      this.hubsToWork.clear();
      ArrayList<Vec3> initPath = new ArrayList();
      initPath.add(this.startVec3);
      this.hubsToWork.add(new AStarCustomPathFinder.Hub(this.startVec3, null, initPath, this.startVec3.squareDistanceTo(this.endVec3), 0.0, 0.0));

      label53:
      for (int i = 0; i < loops; i++) {
         this.hubsToWork.sort(new AStarCustomPathFinder.CompareHub());
         int j = 0;
         if (this.hubsToWork.isEmpty()) {
            break;
         }

         for (AStarCustomPathFinder.Hub o : new ArrayList<>(this.hubsToWork)) {
            if (++j <= depth) {
               this.hubsToWork.remove(o);
               this.hubs.add(o);

               for (Vec3 direction : flatCardinalDirections) {
                  Vec3 loc = o.getLoc().add(direction).floor();
                  if (checkPositionValidity(loc, false) && this.addHub(o, loc, 0.0)) {
                     break label53;
                  }
               }

               Vec3 loc1 = o.getLoc().addVector(0.0, 1.0, 0.0).floor();
               if (checkPositionValidity(loc1, false) && this.addHub(o, loc1, 0.0)) {
                  break label53;
               }

               Vec3 loc2 = o.getLoc().addVector(0.0, -1.0, 0.0).floor();
               if (checkPositionValidity(loc2, false) && this.addHub(o, loc2, 0.0)) {
                  break label53;
               }
            }
         }
      }

      this.hubs.sort(new AStarCustomPathFinder.CompareHub());
      this.path = ((AStarCustomPathFinder.Hub)this.hubs.getFirst()).getPath();
   }

   public AStarCustomPathFinder.Hub isHubExisting(Vec3 loc) {
      for (AStarCustomPathFinder.Hub hub : this.hubs) {
         if (hub.getLoc().x() == loc.x() && hub.getLoc().y() == loc.y() && hub.getLoc().z() == loc.z()) {
            return hub;
         }
      }

      for (AStarCustomPathFinder.Hub hubx : this.hubsToWork) {
         if (hubx.getLoc().x() == loc.x() && hubx.getLoc().y() == loc.y() && hubx.getLoc().z() == loc.z()) {
            return hubx;
         }
      }

      return null;
   }

   public boolean addHub(AStarCustomPathFinder.Hub parent, Vec3 loc, double cost) {
      AStarCustomPathFinder.Hub existingHub = this.isHubExisting(loc);
      double totalCost = cost;
      if (parent != null) {
         totalCost = cost + parent.getTotalCost();
      }

      if (existingHub == null) {
         double minDistanceSquared = 9.0;
         if (loc.x() == this.endVec3.x() && loc.y() == this.endVec3.y() && loc.z() == this.endVec3.z()
            || loc.squareDistanceTo(this.endVec3) <= minDistanceSquared) {
            this.path.clear();
            this.path = parent.getPath();
            this.path.add(loc);
            return true;
         }

         ArrayList<Vec3> path = new ArrayList(parent.getPath());
         path.add(loc);
         this.hubsToWork.add(new AStarCustomPathFinder.Hub(loc, parent, path, loc.squareDistanceTo(this.endVec3), cost, totalCost));
      } else if (existingHub.getCost() > cost) {
         ArrayList<Vec3> path = new ArrayList(parent.getPath());
         path.add(loc);
         existingHub.setLoc(loc);
         existingHub.setParent(parent);
         existingHub.setPath(path);
         existingHub.setSquareDistanceToFromTarget(loc.squareDistanceTo(this.endVec3));
         existingHub.setCost(cost);
         existingHub.setTotalCost(totalCost);
      }

      return false;
   }

   public static class CompareHub implements Comparator<AStarCustomPathFinder.Hub> {
      public int compare(AStarCustomPathFinder.Hub o1, AStarCustomPathFinder.Hub o2) {
         return (int)(o1.getSquareDistanceToFromTarget() + o1.getTotalCost() - (o2.getSquareDistanceToFromTarget() + o2.getTotalCost()));
      }
   }

   public static class Hub {
      private Vec3 loc;
      private AStarCustomPathFinder.Hub parent;
      private ArrayList<Vec3> path;
      private double squareDistanceToFromTarget;
      private double cost;
      private double totalCost;

      public Hub(Vec3 loc, AStarCustomPathFinder.Hub parent, ArrayList<Vec3> path, double squareDistanceToFromTarget, double cost, double totalCost) {
         this.loc = loc;
         this.parent = parent;
         this.path = path;
         this.squareDistanceToFromTarget = squareDistanceToFromTarget;
         this.cost = cost;
         this.totalCost = totalCost;
      }

      public Vec3 getLoc() {
         return this.loc;
      }

      public void setLoc(Vec3 loc) {
         this.loc = loc;
      }

      public AStarCustomPathFinder.Hub getParent() {
         return this.parent;
      }

      public void setParent(AStarCustomPathFinder.Hub parent) {
         this.parent = parent;
      }

      public ArrayList<Vec3> getPath() {
         return this.path;
      }

      public void setPath(ArrayList<Vec3> path) {
         this.path = path;
      }

      public double getSquareDistanceToFromTarget() {
         return this.squareDistanceToFromTarget;
      }

      public void setSquareDistanceToFromTarget(double squareDistanceToFromTarget) {
         this.squareDistanceToFromTarget = squareDistanceToFromTarget;
      }

      public double getCost() {
         return this.cost;
      }

      public void setCost(double cost) {
         this.cost = cost;
      }

      public double getTotalCost() {
         return this.totalCost;
      }

      public void setTotalCost(double totalCost) {
         this.totalCost = totalCost;
      }
   }
}
