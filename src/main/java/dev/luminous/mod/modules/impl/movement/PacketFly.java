package dev.luminous.mod.modules.impl.movement;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.TickEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import io.netty.util.internal.ConcurrentSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

public class PacketFly extends Module {
   public static PacketFly INSTANCE;
   public final EnumSetting<PacketFly.Mode> mode = this.add(new EnumSetting("Mode", PacketFly.Mode.Factor));
   public final SliderSetting factor = this.add(new SliderSetting("Factor", 1.0, 0.0, 10.0));
   public final EnumSetting<PacketFly.Phase> phase = this.add(new EnumSetting("Phase", PacketFly.Phase.Full));
   public final EnumSetting<PacketFly.Type> type = this.add(new EnumSetting("Type", PacketFly.Type.Up));
   public final BooleanSetting antiKick = this.add(new BooleanSetting("AntiKick", true));
   public final BooleanSetting noRotation = this.add(new BooleanSetting("NoRotation", false));
   public final BooleanSetting noMovePacket = this.add(new BooleanSetting("NoMovePacket", false));
   public final BooleanSetting bbOffset = this.add(new BooleanSetting("BB-Offset", false));
   public final SliderSetting invalidY = this.add(new SliderSetting("Invalid-Offset", 1337, 0, 1337));
   public final SliderSetting invalids = this.add(new SliderSetting("Invalids", 1, 0, 10));
   public final SliderSetting sendTeleport = this.add(new SliderSetting("Teleport", 1, 0, 10));
   public final SliderSetting concealY = this.add(new SliderSetting("C-Y", 0.0, -256.0, 256.0));
   public final SliderSetting conceal = this.add(new SliderSetting("C-Multiplier", 1.0, 0.0, 2.0));
   public final SliderSetting ySpeed = this.add(new SliderSetting("Y-Multiplier", 1.0, 0.0, 2.0));
   public final SliderSetting xzSpeed = this.add(new SliderSetting("X/Z-Multiplier", 1.0, 0.0, 2.0));
   public final BooleanSetting elytra = this.add(new BooleanSetting("Elytra", false));
   public final BooleanSetting xzJitter = this.add(new BooleanSetting("Jitter-XZ", false));
   public final BooleanSetting yJitter = this.add(new BooleanSetting("Jitter-Y", false));
   public final BooleanSetting zeroSpeed = this.add(new BooleanSetting("Zero-Speed", false));
   public final BooleanSetting zeroY = this.add(new BooleanSetting("Zero-Y", false));
   public final BooleanSetting zeroTeleport = this.add(new BooleanSetting("Zero-Teleport", true));
   public final SliderSetting zoomer = this.add(new SliderSetting("Zoomies", 3, 0, 10));
   public final Map<Integer, PacketFly.TimeVec> posLooks = new ConcurrentHashMap();
   public final Set<Packet<?>> playerPackets = new ConcurrentSet();
   public final AtomicInteger teleportID = new AtomicInteger();
   public Vec3d vecDelServer;
   public int packetCounter;
   public boolean zoomies;
   public float lastFactor;
   public int zoomTimer = 0;

   public PacketFly() {
      super("PacketFly", Module.Category.Movement);
      this.setChinese("发包飞行");
      INSTANCE = this;
   }

   @Override
   public void onLogin() {
      this.disable();
      this.clearValues();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.posLooks.entrySet().removeIf(entry -> System.currentTimeMillis() - ((PacketFly.TimeVec)entry.getValue()).getTime() > TimeUnit.SECONDS.toMillis(30L));
   }

   @EventListener
   public void invoke(TickEvent event) {
      if (event.isPre() && this.mode.getValue() != PacketFly.Mode.Compatibility) {
         MovementUtil.setMotionX(0.0);
         MovementUtil.setMotionY(0.0);
         MovementUtil.setMotionZ(0.0);
         if (this.mode.getValue() != PacketFly.Mode.Setback && this.teleportID.get() == 0) {
            if (this.checkPackets(6)) {
               this.sendPackets(0.0, 0.0, 0.0, true);
            }

            return;
         }

         boolean isPhasing = this.isPlayerCollisionBoundingBoxEmpty();
         double ySpeed;
         if (!mc.player.input.jumping || !isPhasing && MovementUtil.isMoving()) {
            if (!mc.player.input.sneaking) {
               ySpeed = !isPhasing ? (this.checkPackets(4) ? (this.antiKick.getValue() ? -0.04 : 0.0) : 0.0) : 0.0;
            } else {
               ySpeed = this.yJitter.getValue() && this.zoomies ? -0.061 : -0.062;
            }
         } else if (this.antiKick.getValue() && !isPhasing) {
            ySpeed = this.checkPackets(this.mode.getValue() == PacketFly.Mode.Setback ? 10 : 20) ? -0.032 : 0.062;
         } else {
            ySpeed = this.yJitter.getValue() && this.zoomies ? 0.061 : 0.062;
         }

         if (this.phase.getValue() == PacketFly.Phase.Full && isPhasing && MovementUtil.isMoving() && ySpeed != 0.0) {
            ySpeed /= 2.5;
         }

         double high = this.xzJitter.getValue() && this.zoomies ? 0.25 : 0.26;
         double low = this.xzJitter.getValue() && this.zoomies ? 0.03 : 0.031;
         double[] dirSpeed = MovementUtil.directionSpeed(this.phase.getValue() == PacketFly.Phase.Full && isPhasing ? low : high);
         if (this.mode.getValue() == PacketFly.Mode.Increment) {
            if (this.lastFactor >= this.factor.getValue()) {
               this.lastFactor = 1.0F;
            } else if (++this.lastFactor > this.factor.getValue()) {
               this.lastFactor = this.factor.getValueFloat();
            }
         } else {
            this.lastFactor = this.factor.getValueFloat();
         }

         for (int i = 1;
            i
               <= (
                  this.mode.getValue() != PacketFly.Mode.Factor
                        && this.mode.getValue() != PacketFly.Mode.Slow
                        && this.mode.getValue() != PacketFly.Mode.Increment
                     ? 1.0F
                     : this.lastFactor
               );
            i++
         ) {
            double conceal = mc.player.getY() < this.concealY.getValue() && MovementUtil.isMoving() ? this.conceal.getValue() : 1.0;
            MovementUtil.setMotionX(dirSpeed[0] * i * conceal * this.xzSpeed.getValue());
            MovementUtil.setMotionY(ySpeed * i * this.ySpeed.getValue());
            MovementUtil.setMotionZ(dirSpeed[1] * i * conceal * this.xzSpeed.getValue());
            this.sendPackets(MovementUtil.getMotionX(), MovementUtil.getMotionY(), MovementUtil.getMotionZ(), this.mode.getValue() != PacketFly.Mode.Setback);
         }

         this.zoomTimer++;
         if (this.zoomTimer > this.zoomer.getValue()) {
            this.zoomies = !this.zoomies;
            this.zoomTimer = 0;
         }
      }
   }

   @EventListener
   public void invoke(PacketEvent.Receive event) {
      if (!nullCheck()) {
         if (this.mode.getValue() != PacketFly.Mode.Compatibility) {
            if (event.getPacket() instanceof PlayerPositionLookS2CPacket packet) {
               if (mc.player.isAlive()
                  && this.mode.getValue() != PacketFly.Mode.Setback
                  && this.mode.getValue() != PacketFly.Mode.Slow
                  && !(mc.currentScreen instanceof DownloadingTerrainScreen)) {
                  PacketFly.TimeVec vec = (PacketFly.TimeVec)this.posLooks.remove(packet.getTeleportId());
                  if (vec != null
                     && vec.x == packet.getX()
                     && vec.y == packet.getY()
                     && vec.z == packet.getZ()) {
                     event.setCancelled(true);
                     return;
                  }
               }

               this.teleportID.set(packet.getTeleportId());
            }
         }
      }
   }

   @EventListener
   public void invoke(MoveEvent event) {
      if (this.phase.getValue() == PacketFly.Phase.Semi || this.isPlayerCollisionBoundingBoxEmpty()) {
         mc.player.noClip = true;
      }

      if (this.mode.getValue() != PacketFly.Mode.Compatibility && (this.mode.getValue() == PacketFly.Mode.Setback || this.teleportID.get() != 0)) {
         if (this.zeroSpeed.getValue()) {
            event.setX(0.0);
            event.setY(0.0);
            event.setZ(0.0);
         } else {
            event.setX(MovementUtil.getMotionX());
            event.setY(MovementUtil.getMotionY());
            event.setZ(MovementUtil.getMotionZ());
         }

         if (this.zeroY.getValue()) {
            event.setY(0.0);
         }
      }
   }

   @EventListener
   public void onPacket(PacketEvent.Send event) {
      if (event.getPacket() instanceof PlayerMoveC2SPacket packet
         && this.mode.getValue() != PacketFly.Mode.Compatibility
         && !this.playerPackets.remove(event.getPacket())) {
         if (packet instanceof LookAndOnGround && !this.noRotation.getValue()) {
            return;
         }

         if (!this.noMovePacket.getValue()) {
            return;
         }

         event.setCancelled(true);
      }
   }

   @Override
   public void onEnable() {
      this.clearValues();
      if (mc.player == null) {
         this.disable();
      }
   }

   @Override
   public String getInfo() {
      return ((PacketFly.Mode)this.mode.getValue()).toString();
   }

   public void clearValues() {
      this.lastFactor = 1.0F;
      this.packetCounter = 0;
      this.teleportID.set(0);
      this.playerPackets.clear();
      this.posLooks.clear();
      this.vecDelServer = null;
   }

   public boolean isPlayerCollisionBoundingBoxEmpty() {
      double o = this.bbOffset.getValue() ? -0.0625 : 0.0;
      return BlockUtil.canCollide(mc.player, mc.player.getBoundingBox().expand(o, o, o));
   }

   public boolean checkPackets(int amount) {
      if (++this.packetCounter >= amount) {
         this.packetCounter = 0;
         return true;
      } else {
         return false;
      }
   }

   public void sendPackets(double x, double y, double z, boolean confirm) {
      Vec3d offset = new Vec3d(x, y, z);
      Vec3d vec = mc.player.getPos().add(offset);
      this.vecDelServer = vec;
      Vec3d oOB = ((PacketFly.Type)this.type.getValue()).createOutOfBounds(vec, this.invalidY.getValueInt());
      this.sendCPacket(new PositionAndOnGround(vec.x, vec.y, vec.z, mc.player.isOnGround()));
      if (!mc.isInSingleplayer()) {
         for (int i = 0; i < this.invalids.getValue(); i++) {
            this.sendCPacket(new PositionAndOnGround(oOB.x, oOB.y, oOB.z, mc.player.isOnGround()));
            oOB = ((PacketFly.Type)this.type.getValue()).createOutOfBounds(oOB, this.invalidY.getValueInt());
         }
      }

      if (confirm && (this.zeroTeleport.getValue() || this.teleportID.get() != 0)) {
         for (int i = 0; i < this.sendTeleport.getValue(); i++) {
            this.sendConfirmTeleport(vec);
         }
      }

      if (this.elytra.getValue()) {
         mc.getNetworkHandler()
            .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
      }
   }

   public void sendConfirmTeleport(Vec3d vec) {
      int id = this.teleportID.incrementAndGet();
      mc.getNetworkHandler().sendPacket(new TeleportConfirmC2SPacket(id));
      this.posLooks.put(id, new PacketFly.TimeVec(vec));
   }

   public void sendCPacket(Packet<?> packet) {
      this.playerPackets.add(packet);
      mc.getNetworkHandler().sendPacket(packet);
   }

   public static enum Mode {
      Setback,
      Fast,
      Factor,
      Slow,
      Increment,
      Compatibility;
   }

   public static enum Phase {
      Off,
      Semi,
      Full;
   }

   public static class TimeVec extends Vec3d {
      final long time;

      public TimeVec(Vec3d vec3d) {
         this(vec3d.x, vec3d.y, vec3d.z, System.currentTimeMillis());
      }

      public TimeVec(double xIn, double yIn, double zIn, long time) {
         super(xIn, yIn, zIn);
         this.time = time;
      }

      public long getTime() {
         return this.time;
      }
   }

   public static enum Type {
      Down {
         @Override
         public Vec3d createOutOfBounds(Vec3d vec3d, int invalid) {
            return vec3d.add(0.0, -invalid, 0.0);
         }
      },
      Up {
         @Override
         public Vec3d createOutOfBounds(Vec3d vec3d, int invalid) {
            return vec3d.add(0.0, invalid, 0.0);
         }
      },
      Preserve {
         final Random random = new Random();

         private int randomInt() {
            int result = this.random.nextInt(29000000);
            return this.random.nextBoolean() ? result : -result;
         }

         @Override
         public Vec3d createOutOfBounds(Vec3d vec3d, int invalid) {
            return vec3d.add(this.randomInt(), 0.0, this.randomInt());
         }
      },
      Switch {
         final Random random = new Random();

         @Override
         public Vec3d createOutOfBounds(Vec3d vec3d, int invalid) {
            boolean down = this.random.nextBoolean();
            return down ? vec3d.add(0.0, -invalid, 0.0) : vec3d.add(0.0, invalid, 0.0);
         }
      },
      X {
         @Override
         public Vec3d createOutOfBounds(Vec3d vec3d, int invalid) {
            return vec3d.add(invalid, 0.0, 0.0);
         }
      },
      Z {
         @Override
         public Vec3d createOutOfBounds(Vec3d vec3d, int invalid) {
            return vec3d.add(0.0, 0.0, invalid);
         }
      },
      XZ {
         @Override
         public Vec3d createOutOfBounds(Vec3d vec3d, int invalid) {
            return vec3d.add(invalid, 0.0, invalid);
         }
      };

      public abstract Vec3d createOutOfBounds(Vec3d var1, int var2);
   }
}
