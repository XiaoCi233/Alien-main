package dev.luminous.api.utils.math;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.asm.accessors.IEntity;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.client.AntiCheat.Motion;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class PredictUtil implements Wrapper {
   public static Vec3d getPos(PlayerEntity entity, int ticks) {
      return ticks <= 0
         ? entity.getPos()
         : getPos(
            entity,
            AntiCheat.INSTANCE.maxMotionY.getValue(),
            AntiCheat.INSTANCE.predictTicks.getValueInt(),
            AntiCheat.INSTANCE.simulation.getValueInt(),
            AntiCheat.INSTANCE.step.getValue(),
            AntiCheat.INSTANCE.doubleStep.getValue(),
            AntiCheat.INSTANCE.jump.getValue(),
            AntiCheat.INSTANCE.inBlockPause.getValue()
         );
   }

   public static Vec3d getPos(
      PlayerEntity e, double maxMotionY, int ticks, int simulation, boolean step, boolean doubleStep, boolean jump, boolean inBlockPause
   ) {
      if (inBlockPause && BlockUtil.canCollide(e, e.getBoundingBox())) {
         return e.getPos();
      } else {
         double velocityX;
         double velocityY;
         double velocityZ;
         if (AntiCheat.INSTANCE.motion.is(AntiCheat.Motion.Position)) {
            velocityX = e.getX() - e.prevX;
            velocityY = e.getY() - e.prevY;
            velocityZ = e.getZ() - e.prevZ;
            if (velocityY > maxMotionY) {
               velocityY = maxMotionY;
            }
         } else {
            velocityX = e.getVelocity().x;
            velocityY = e.getVelocity().y;
            velocityZ = e.getVelocity().z;
         }

         double motionX = velocityX;
         double motionY = velocityY;
         double motionZ = velocityZ;
         double x = e.getX();
         double y = e.getY();
         double z = e.getZ();
         Vec3d lastPos = new Vec3d(x, y, z);
         if (velocityX == 0.0 && velocityY == 0.0 && velocityZ == 0.0) {
            return lastPos;
         } else {
            for (int i = 0; i < ticks; i++) {
               lastPos = new Vec3d(x, y, z);
               boolean move = false;
               boolean fall = false;
               int yTime = simulation;

               label140:
               while (true) {
                  if (yTime >= 0) {
                     int xTime = simulation;

                     label135:
                     while (true) {
                        if (xTime < 0) {
                           yTime--;
                           continue label140;
                        }

                        double xFactor = (double)xTime / simulation;
                        double yFactor = (double)yTime / simulation;
                        if (canMove(lastPos.add(motionX * xFactor, motionY * yFactor, motionZ * xFactor), e)) {
                           if (Math.abs(motionX * xFactor) + Math.abs(motionZ * xFactor) + Math.abs(motionY * yFactor) <= 0.05) {
                              if (step && !canMove(lastPos.add(velocityX, 0.0, velocityZ), e) && canMove(lastPos.add(velocityX, 1.1, velocityZ), e)) {
                                 y++;
                                 motionY = 0.03;

                                 for (int yTime2 = simulation; yTime2 >= 0; yTime2--) {
                                    for (int xTime2 = simulation; xTime2 >= 0; xTime2--) {
                                       double xFactor2 = (double)xTime2 / simulation;
                                       double yFactor2 = (double)yTime2 / simulation;
                                       if (canMove(lastPos.add(motionX * xFactor2, motionY * yFactor2, motionZ * xFactor2), e)) {
                                          move = true;
                                          x += motionX * xFactor2;
                                          z += motionZ * xFactor2;
                                          if (yTime2 > 0) {
                                             y += motionY * yFactor2;
                                             fall = true;
                                          }
                                          break label135;
                                       }
                                    }
                                 }

                                 return lastPos;
                              } else {
                                 if (!doubleStep || canMove(lastPos.add(velocityX, 0.0, velocityZ), e) || !canMove(lastPos.add(velocityX, 2.1, velocityZ), e)) {
                                    return lastPos;
                                 }

                                 y += 2.05;
                                 motionY = 0.03;

                                 for (int yTime2 = simulation; yTime2 >= 0; yTime2--) {
                                    for (int xTime2x = simulation; xTime2x >= 0; xTime2x--) {
                                       double xFactor2 = (double)xTime2x / simulation;
                                       double yFactor2 = (double)yTime2 / simulation;
                                       if (canMove(lastPos.add(motionX * xFactor2, motionY * yFactor2, motionZ * xFactor2), e)) {
                                          move = true;
                                          x += motionX * xFactor2;
                                          z += motionZ * xFactor2;
                                          if (yTime2 > 0) {
                                             y += motionY * yFactor2;
                                             fall = true;
                                          }
                                          break label135;
                                       }
                                    }
                                 }

                                 return lastPos;
                              }
                           } else {
                              move = true;
                              x += motionX * xFactor;
                              z += motionZ * xFactor;
                              if (yTime > 0) {
                                 y += motionY * yFactor;
                                 fall = true;
                              }
                              break;
                           }
                        }

                        xTime--;
                     }
                  }

                  if (!move) {
                     return lastPos;
                  }

                  if (!e.isFallFlying()) {
                     motionX *= 0.99;
                     motionZ *= 0.99;
                     motionY *= 0.99;
                     motionY -= 0.05F;
                  }

                  if (!fall) {
                     if (e.isOnGround()) {
                        motionX = velocityX;
                        motionZ = velocityZ;
                        motionY = 0.0;
                     } else if (jump) {
                        motionX = velocityX;
                        motionZ = velocityZ;
                        motionY = 0.333;
                     } else {
                        motionY = 0.0;
                     }
                  }
                  break;
               }
            }

            return lastPos;
         }
      }
   }

   public static boolean canMove(Vec3d pos, PlayerEntity player) {
      return !BlockUtil.canCollide(player, ((IEntity)player).getDimensions().getBoxAt(pos)) || new Box(new BlockPosX(pos)).intersects(player.getBoundingBox());
   }
}
