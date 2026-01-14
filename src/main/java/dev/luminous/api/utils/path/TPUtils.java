package dev.luminous.api.utils.path;

import com.google.common.collect.Lists;
import dev.luminous.api.utils.Wrapper;
import java.util.List;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.math.Vec3d;

public class TPUtils implements Wrapper {
   public static void teleportWithBack(Vec3d newPos, TPUtils.TeleportType type, Runnable runnable) {
      switch (type) {
         case Legacy:
            legacyTeleportWithBack(newPos, runnable);
            break;
         case New:
            newTeleportWithBack(newPos, runnable);
      }
   }

   public static void legacyTeleportWithBack(Vec3d newPos, Runnable runnable) {
      List<Vec3> tpPath = PathUtils.computePath(newPos);
      tpPath.removeFirst();
      tpPath.forEach(vec3 -> mc.player.networkHandler.sendPacket(new PositionAndOnGround(vec3.x(), vec3.y(), vec3.z(), false)));
      runnable.run();
      tpPath = Lists.reverse(tpPath);
      tpPath.removeFirst();
      tpPath.forEach(vec3 -> mc.player.networkHandler.sendPacket(new PositionAndOnGround(vec3.x(), vec3.y(), vec3.z(), false)));
      mc.player.networkHandler.sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
   }

   public static void newTeleportWithBack(Vec3d newPos, Runnable runnable) {
      int packetsRequired = (int)Math.ceil(mc.player.getPos().distanceTo(newPos) / 10.0) - 1;

      for (int i = 0; i < packetsRequired; i++) {
         mc.player.networkHandler.sendPacket(new OnGroundOnly(true));
      }

      mc.player.networkHandler.sendPacket(new PositionAndOnGround(newPos.x, newPos.y, newPos.z, true));
      runnable.run();

      for (int i = 0; i < packetsRequired; i++) {
         mc.player.networkHandler.sendPacket(new OnGroundOnly(true));
      }

      mc.player.networkHandler.sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
   }

   public static void newTeleport(Vec3d newPos) {
      int packetsRequired = (int)Math.ceil(mc.player.getPos().distanceTo(newPos) / 10.0) - 1;

      for (int i = 0; i < packetsRequired; i++) {
         mc.player.networkHandler.sendPacket(new OnGroundOnly(true));
      }

      mc.player.networkHandler.sendPacket(new PositionAndOnGround(newPos.x, newPos.y, newPos.z, true));
   }

   public static enum TeleportType {
      Legacy,
      New;
   }
}
