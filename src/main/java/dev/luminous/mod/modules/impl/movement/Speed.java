package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.events.impl.MovedEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.path.BaritoneUtil;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class Speed extends Module {
   public static Speed INSTANCE;
   private final EnumSetting<Speed.Mode> mode = this.add(new EnumSetting("Mode", Speed.Mode.Strafe));
   public final SliderSetting collideSpeed = this.add(new SliderSetting("CollideSpeed", 0.08, 0.0, 0.08, 0.01, () -> this.mode.is(Speed.Mode.Grim)));
   private final BooleanSetting strict = this.add(new BooleanSetting("Strict", true, () -> this.mode.is(Speed.Mode.Grim)));
   private final BooleanSetting boat = this.add(new BooleanSetting("BoatLongJump", true, () -> this.mode.is(Speed.Mode.Grim)));
   public final SliderSetting boatExpand = this.add(new SliderSetting("BoatExpand", 0.2, 0.0, 1.0, 0.01, () -> this.mode.is(Speed.Mode.Grim)));
   public final SliderSetting boatSpeed = this.add(new SliderSetting("BoatSpeed", 0.2, -2.0, 2.0, 0.01, () -> this.mode.is(Speed.Mode.Grim)));
   public final SliderSetting boatJump = this.add(new SliderSetting("BoatJump", 0.2, 0.0, 2.0, 0.01, () -> this.mode.is(Speed.Mode.Grim)));
   private final BooleanSetting inWater = this.add(new BooleanSetting("InWater", false, () -> !this.mode.is(Speed.Mode.Grim)));
   private final BooleanSetting inBlock = this.add(new BooleanSetting("InBlock", false, () -> !this.mode.is(Speed.Mode.Grim)));
   private final BooleanSetting airStop = this.add(new BooleanSetting("AirStop", false, () -> !this.mode.is(Speed.Mode.Grim)));
   private final SliderSetting lagTime = this.add(new SliderSetting("LagTime", 500.0, 0.0, 1000.0, 1.0, () -> !this.mode.is(Speed.Mode.Grim)));
   private final BooleanSetting jump = this.add(new BooleanSetting("Jump", true, () -> this.mode.is(Speed.Mode.Strafe)));
   private final SliderSetting strafeSpeed = this.add(new SliderSetting("Speed", 0.2873, 0.0, 1.0, 1.0E-4, () -> this.mode.is(Speed.Mode.Strafe)));
   private final BooleanSetting explosions = this.add(new BooleanSetting("ExplosionsBoost", false, () -> this.mode.is(Speed.Mode.Strafe)));
   private final BooleanSetting velocity = this.add(new BooleanSetting("VelocityBoost", true, () -> this.mode.is(Speed.Mode.Strafe)));
   private final SliderSetting multiplier = this.add(new SliderSetting("H-Factor", 1.0, 0.0, 5.0, 0.01, () -> this.mode.is(Speed.Mode.Strafe)));
   private final SliderSetting vertical = this.add(new SliderSetting("V-Factor", 1.0, 0.0, 5.0, 0.01, () -> this.mode.is(Speed.Mode.Strafe)));
   private final SliderSetting coolDown = this.add(new SliderSetting("CoolDown", 1000.0, 0.0, 5000.0, 1.0, () -> this.mode.is(Speed.Mode.Strafe)));
   private final BooleanSetting slow = this.add(new BooleanSetting("Slowness", false, () -> this.mode.is(Speed.Mode.Strafe)));
   private final Timer expTimer = new Timer();
   private final Timer lagTimer = new Timer();
   private boolean stop;
   private double speed;
   private double distance;
   private int strictTicks;
   private int strafe = 4;
   private int stage;
   private double lastExp;
   private boolean boost;

   public Speed() {
      super("Speed", Module.Category.Movement);
      this.setChinese("加速");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      return ((Speed.Mode)this.mode.getValue()).name();
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.speed = MovementUtil.getSpeed(false);
         this.distance = MovementUtil.getDistance2D();
      }

      this.stage = 4;
   }

   @EventListener(priority = 100)
   public void invoke(PacketEvent.Receive event) {
      if (!BaritoneUtil.isActive()) {
         if (this.mode.is(Speed.Mode.Strafe)) {
            if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
               if (mc.player != null && packet.getEntityId() == mc.player.getId() && this.velocity.getValue()) {
                  double speed = Math.sqrt(packet.getVelocityX() * packet.getVelocityX() + packet.getVelocityZ() * packet.getVelocityZ());
                  this.lastExp = this.expTimer.passed(this.coolDown.getValueInt()) ? speed : speed - this.lastExp;
                  if (this.lastExp > 0.0) {
                     this.expTimer.reset();
                     this.speed = this.speed + this.lastExp * this.multiplier.getValue();
                     this.distance = this.distance + this.lastExp * this.multiplier.getValue();
                     if (MovementUtil.getMotionY() > 0.0 && this.vertical.getValue() != 0.0) {
                        MovementUtil.setMotionY(MovementUtil.getMotionY() * this.vertical.getValue());
                     }
                  }
               }
            } else if (event.getPacket() instanceof ExplosionS2CPacket packetx
               && this.explosions.getValue()
               && mc.player.getPos().distanceTo(new Vec3d(packetx.getX(), packetx.getY(), packetx.getZ())) < 15.0) {
               double speed = Math.sqrt(packetx.getPlayerVelocityX() * packetx.getPlayerVelocityX() + packetx.getPlayerVelocityZ() * packetx.getPlayerVelocityZ());
               this.lastExp = this.expTimer.passed(this.coolDown.getValueInt()) ? speed : speed - this.lastExp;
               if (this.lastExp > 0.0) {
                  this.expTimer.reset();
                  this.speed = this.speed + this.lastExp * this.multiplier.getValue();
                  this.distance = this.distance + this.lastExp * this.multiplier.getValue();
                  if (MovementUtil.getMotionY() > 0.0) {
                     MovementUtil.setMotionY(MovementUtil.getMotionY() * this.vertical.getValue());
                  }
               }
            }
         }

         if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.lagTimer.reset();
            this.resetStrafe();
         }
      }
   }

   @EventListener
   public void onMove(MovedEvent event) {
      if (!nullCheck()) {
         double dx = mc.player.getX() - mc.player.prevX;
         double dz = mc.player.getZ() - mc.player.prevZ;
         this.distance = Math.sqrt(dx * dx + dz * dz);
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.mode.is(Speed.Mode.Grim)) {
         if (!MovementUtil.isMoving()) {
            return;
         }

         int collisions = 0;
         Box box = this.strict.getValue() ? mc.player.getBoundingBox() : mc.player.getBoundingBox().expand(1.0);

         for (Entity entity : Alien.THREAD.getEntities()) {
            Box entityBox = entity.getBoundingBox();
            if (this.boat.getValue()
               && mc.player.isOnGround()
               && entity instanceof BoatEntity
               && box.intersects(entityBox.expand(this.boatExpand.getValue()))) {
               double yaw = Math.toRadians(Sprint.getSprintYaw(mc.player.getYaw()));
               double boost = this.boatSpeed.getValue();
               mc.player.setVelocity(-Math.sin(yaw) * boost, this.boatJump.getValue(), Math.cos(yaw) * boost);
               return;
            }

            if (box.intersects(entityBox) && this.canCauseSpeed(entity)) {
               collisions++;
            }
         }

         double yaw = Math.toRadians(Sprint.getSprintYaw(mc.player.getYaw()));
         double boost = this.collideSpeed.getValue() * collisions;
         mc.player.addVelocity(-Math.sin(yaw) * boost, 0.0, Math.cos(yaw) * boost);
      }
   }

   private boolean canCauseSpeed(Entity entity) {
      return entity != mc.player && entity instanceof LivingEntity && !(entity instanceof ArmorStandEntity);
   }

   @EventListener
   public void invoke(MoveEvent event) {
      if (!MovementUtil.isMoving() && this.airStop.getValue() && !this.mode.is(Speed.Mode.Grim)) {
         MovementUtil.setMotionX(0.0);
         MovementUtil.setMotionZ(0.0);
      }

      if ((this.inWater.getValue() || !mc.player.isSubmergedInWater() && !mc.player.isTouchingWater() && !mc.player.isInLava())
         && !mc.player.isRiding()
         && !mc.player.isHoldingOntoLadder()
         && (this.inBlock.getValue() || !EntityUtil.isInsideBlock())
         && !mc.player.getAbilities().flying
         && !mc.player.isFallFlying()
         && MovementUtil.isMoving()) {
         if (!this.mode.is(Speed.Mode.Strafe)) {
            double speedEffect = 1.0;
            double slowEffect = 1.0;
            if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
               double amplifier = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
               speedEffect = 1.0 + 0.2 * (amplifier + 1.0);
            }

            if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
               double amplifier = mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
               slowEffect = 1.0 + 0.2 * (amplifier + 1.0);
            }

            double base = 0.2873F * speedEffect / slowEffect;
            float jumpEffect = 0.0F;
            if (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
               jumpEffect += (mc.player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1F;
            }

            if (this.mode.getValue() == Speed.Mode.StrafeStrict) {
               if (!this.lagTimer.passed(this.lagTime.getValueInt())) {
                  return;
               }

               if (this.strafe == 1) {
                  this.speed = 1.35F * base - 0.01F;
               } else if (this.strafe == 2) {
                  if (mc.player.input.jumping || !mc.player.isOnGround()) {
                     return;
                  }

                  float jump = 0.39999995F + jumpEffect;
                  event.setY(jump);
                  MovementUtil.setMotionY(jump);
                  this.speed *= 2.149;
               } else if (this.strafe == 3) {
                  double moveSpeed = 0.66 * (this.distance - base);
                  this.speed = this.distance - moveSpeed;
               } else {
                  if ((
                        !mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().getY(), 0.0))
                           || mc.player.verticalCollision
                     )
                     && this.strafe > 0) {
                     this.strafe = 1;
                  }

                  this.speed = this.distance - this.distance / 159.0;
               }

               this.strictTicks++;
               this.speed = Math.max(this.speed, base);
               double baseMax = 0.465 * speedEffect / slowEffect;
               double baseMin = 0.44 * speedEffect / slowEffect;
               this.speed = Math.min(this.speed, this.strictTicks > 25 ? baseMax : baseMin);
               if (this.strictTicks > 50) {
                  this.strictTicks = 0;
               }

               Vec2f motion = this.handleStrafeMotion((float)this.speed);
               event.setX(motion.x);
               event.setZ(motion.y);
               this.strafe++;
            }
         } else if (this.stop) {
            this.stop = false;
         } else if (this.lagTimer.passed(this.lagTime.getValueInt())) {
            if (this.stage == 1) {
               this.speed = 1.35 * MovementUtil.getSpeed(this.slow.getValue(), this.strafeSpeed.getValue()) - 0.01;
            } else if (this.stage != 2 || !mc.player.isOnGround() || !mc.options.jumpKey.isPressed() && !this.jump.getValue()) {
               if (this.stage == 3) {
                  this.speed = this.distance - 0.66 * (this.distance - MovementUtil.getSpeed(this.slow.getValue(), this.strafeSpeed.getValue()));
                  this.boost = !this.boost;
               } else {
                  if ((BlockUtil.canCollide(null, mc.player.getBoundingBox().offset(0.0, MovementUtil.getMotionY(), 0.0)) || mc.player.collidedSoftly)
                     && this.stage > 0) {
                     this.stage = 1;
                  }

                  this.speed = this.distance - this.distance / 159.0;
               }
            } else {
               double yMotion = 0.3999 + MovementUtil.getJumpSpeed();
               MovementUtil.setMotionY(yMotion);
               event.setY(yMotion);
               this.speed = this.speed * (this.boost ? 1.6835 : 1.395);
            }

            this.speed = Math.min(this.speed, 10.0);
            this.speed = Math.max(this.speed, MovementUtil.getSpeed(this.slow.getValue(), this.strafeSpeed.getValue()));
            double n = mc.player.input.movementForward;
            double n2 = mc.player.input.movementSideways;
            double n3 = mc.player.getYaw();
            if (n == 0.0 && n2 == 0.0) {
               event.setX(0.0);
               event.setZ(0.0);
            } else if (n != 0.0 && n2 != 0.0) {
               n *= Math.sin(Math.PI / 4);
               n2 *= Math.cos(Math.PI / 4);
            }

            event.setX((n * this.speed * -Math.sin(Math.toRadians(n3)) + n2 * this.speed * Math.cos(Math.toRadians(n3))) * 0.99);
            event.setZ((n * this.speed * Math.cos(Math.toRadians(n3)) - n2 * this.speed * -Math.sin(Math.toRadians(n3))) * 0.99);
            this.stage++;
         }
      } else {
         this.resetStrafe();
         this.stop = true;
      }
   }

   public Vec2f handleStrafeMotion(float speed) {
      float forward = mc.player.input.movementForward;
      float strafe = mc.player.input.movementSideways;
      float yaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw) * mc.getRenderTickCounter().getTickDelta(true);
      if (forward == 0.0F && strafe == 0.0F) {
         return Vec2f.ZERO;
      } else {
         if (forward != 0.0F) {
            if (strafe >= 1.0F) {
               yaw += forward > 0.0F ? -45.0F : 45.0F;
               strafe = 0.0F;
            } else if (strafe <= -1.0F) {
               yaw += forward > 0.0F ? 45.0F : -45.0F;
               strafe = 0.0F;
            }

            if (forward > 0.0F) {
               forward = 1.0F;
            } else if (forward < 0.0F) {
               forward = -1.0F;
            }
         }

         float rx = (float)Math.cos(Math.toRadians(yaw));
         float rz = (float)(-Math.sin(Math.toRadians(yaw)));
         return new Vec2f(forward * speed * rz + strafe * speed * rx, forward * speed * rx - strafe * speed * rz);
      }
   }

   public void resetStrafe() {
      this.strafe = 4;
      this.strictTicks = 0;
      this.speed = 0.0;
      this.distance = 0.0;
   }

   public static enum Mode {
      Strafe,
      StrafeStrict,
      Grim;
   }
}
