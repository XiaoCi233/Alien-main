package dev.luminous.api.utils.math;

import dev.luminous.api.utils.Wrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class MathUtil implements Wrapper {
   public static float clamp(float num, float min, float max) {
      return num < min ? min : Math.min(num, max);
   }

   public static double clamp(double value, double min, double max) {
      return value < min ? min : Math.min(value, max);
   }

   public static double round(double value, int places) {
      BigDecimal bd = new BigDecimal(value);
      bd = bd.setScale(places, RoundingMode.HALF_UP);
      return bd.doubleValue();
   }

   public static double square(double input) {
      return input * input;
   }

   public static float random(float min, float max) {
      return (float)(Math.random() * (max - min) + min);
   }

   public static double random(double min, double max) {
      return (float)(Math.random() * (max - min) + min);
   }

   public static float rad(float angle) {
      return (float)(angle * Math.PI / 180.0);
   }

   public static double interpolate(double previous, double current, double delta) {
      return previous + (current - previous) * delta;
   }

   public static float interpolate(float previous, float current, float delta) {
      return previous + (current - previous) * delta;
   }

   public static Direction getFacingOrder(float yaw, float pitch) {
      float f = pitch * (float) (Math.PI / 180.0);
      float g = -yaw * (float) (Math.PI / 180.0);
      float h = MathHelper.sin(f);
      float i = MathHelper.cos(f);
      float j = MathHelper.sin(g);
      float k = MathHelper.cos(g);
      boolean bl = j > 0.0F;
      boolean bl2 = h < 0.0F;
      boolean bl3 = k > 0.0F;
      float l = bl ? j : -j;
      float m = bl2 ? -h : h;
      float n = bl3 ? k : -k;
      float o = l * i;
      float p = n * i;
      Direction direction = bl ? Direction.EAST : Direction.WEST;
      Direction direction2 = bl2 ? Direction.UP : Direction.DOWN;
      Direction direction3 = bl3 ? Direction.SOUTH : Direction.NORTH;
      if (l > n) {
         return m > o ? direction2 : direction;
      } else {
         return m > p ? direction2 : direction3;
      }
   }

   public static Direction getDirectionFromEntityLiving(BlockPos pos, LivingEntity entity) {
      if (Math.abs(entity.getX() - (pos.getX() + 0.5)) < 2.0 && Math.abs(entity.getZ() - (pos.getZ() + 0.5)) < 2.0) {
         double d0 = entity.getY() + entity.getEyeHeight(entity.getPose());
         if (d0 - pos.getY() > 2.0) {
            return Direction.UP;
         }

         if (pos.getY() - d0 > 0.0) {
            return Direction.DOWN;
         }
      }

      return entity.getHorizontalFacing().getOpposite();
   }

   public static Vec3d getRenderPosition(Entity entity) {
      return getRenderPosition(entity, mc.getRenderTickCounter().getTickDelta(true));
   }

   public static Vec3d getRenderPosition(Entity entity, float tickDelta) {
      return new Vec3d(
         entity.prevX + (entity.getX() - entity.prevX) * tickDelta,
         entity.prevY + (entity.getY() - entity.prevY) * tickDelta,
         entity.prevZ + (entity.getZ() - entity.prevZ) * tickDelta
      );
   }

   public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
      double dX = x2 - x1;
      double dY = y2 - y1;
      double dZ = z2 - z1;
      return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
   }

   public static Vec3d getPointToBoxFromBottom(Vec3d eye, Vec3d bottom, double range, double boxSize, double step) {
      Vec3d target = null;
      double halfWidth = boxSize / 2.0;
      double minOffset = Double.MAX_VALUE;
      boolean xPlus = bottom.x < eye.x;
      boolean zPlus = bottom.z < eye.z;
      double rangeSq = range * range;
      double yOffset = 0.0;

      while (yOffset <= boxSize) {
         for (double xOffset = 0.0; xOffset <= halfWidth; xOffset += step) {
            for (double zOffset = 0.0; zOffset <= halfWidth; zOffset += step) {
               double y = bottom.y + yOffset;
               if (yOffset != 0.0 && y > eye.y) {
                  return target;
               }

               double x = bottom.x + (xPlus ? xOffset : -xOffset);
               double z = bottom.z + (zPlus ? zOffset : -zOffset);
               double dxToEye = x - eye.x;
               double dyToEye = y - eye.y;
               double dzToEye = z - eye.z;
               double distSq = dxToEye * dxToEye + dyToEye * dyToEye + dzToEye * dzToEye;
               double offsets = xOffset + yOffset + zOffset;
               if (distSq <= rangeSq && offsets < minOffset) {
                  minOffset = offsets;
                  target = new Vec3d(x, y, z);
               }
            }
         }

         yOffset += step;
      }

      return target;
   }

   public static Vec3d getClosestPointToBox(Vec3d pos, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      double closestX = Math.max(minX, Math.min(pos.x, maxX));
      double closestY = Math.max(minY, Math.min(pos.y, maxY));
      double closestZ = Math.max(minZ, Math.min(pos.z, maxZ));
      return new Vec3d(closestX, closestY, closestZ);
   }

   public static Vec3d getClosestPointToBox(Vec3d eyePos, Box boundingBox) {
      return getClosestPointToBox(eyePos, boundingBox.minX, boundingBox.minY, boundingBox.minZ, boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
   }

   public static Vec3d getClosestPoint(Entity entity) {
      return getClosestPointToBox(mc.player.getEyePos(), entity.getBoundingBox());
   }
}
