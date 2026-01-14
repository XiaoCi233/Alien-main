package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.RotationEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.JelloUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.asm.accessors.IEntity;
import dev.luminous.asm.accessors.ILivingEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.exploit.Blink;
import dev.luminous.mod.modules.impl.movement.ElytraFly;
import dev.luminous.mod.modules.impl.movement.Velocity;
import dev.luminous.mod.modules.settings.enums.SwingSide;
import dev.luminous.mod.modules.settings.enums.Timing;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class Aura extends Module {
   public static Aura INSTANCE;
   public static Entity target;
   public final EnumSetting<Aura.Page> page = this.add(new EnumSetting("Page", Aura.Page.General));
   public final SliderSetting range = this.add(new SliderSetting("Range", 6.0, 0.1F, 7.0, () -> this.page.getValue() == Aura.Page.General));
   private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 8.0, 0.1F, 14.0, () -> this.page.getValue() == Aura.Page.General));
   private final EnumSetting<Aura.Cooldown> cooldownMode = this.add(
      new EnumSetting("CooldownMode", Aura.Cooldown.Delay, () -> this.page.getValue() == Aura.Page.General)
   );
   private final BooleanSetting reset = this.add(
      new BooleanSetting("Reset", true, () -> this.page.getValue() == Aura.Page.General && this.cooldownMode.is(Aura.Cooldown.Delay))
   );
   private final EnumSetting<SwingSide> swingMode = this.add(new EnumSetting("Swing", SwingSide.All, () -> this.page.getValue() == Aura.Page.General));
   private final SliderSetting hurtTime = this.add(new SliderSetting("HurtTime", 10.0, 0.0, 10.0, 1.0, () -> this.page.getValue() == Aura.Page.General));
   private final SliderSetting cooldown = this.add(new SliderSetting("Cooldown", 1.1F, 0.0, 1.2F, 0.01, () -> this.page.getValue() == Aura.Page.General));
   private final SliderSetting wallRange = this.add(new SliderSetting("WallRange", 6.0, 0.1F, 7.0, () -> this.page.getValue() == Aura.Page.General));
   private final BooleanSetting whileEating = this.add(new BooleanSetting("WhileUsing", true, () -> this.page.getValue() == Aura.Page.General));
   private final BooleanSetting weaponOnly = this.add(new BooleanSetting("WeaponOnly", true, () -> this.page.getValue() == Aura.Page.General));
   private final EnumSetting<Timing> timing = this.add(new EnumSetting("Timing", Timing.All, () -> this.page.getValue() == Aura.Page.General));
   private final BooleanSetting Players = this.add(new BooleanSetting("Players", true, () -> this.page.getValue() == Aura.Page.Target).setParent());
   private final BooleanSetting armorLow = this.add(
      new BooleanSetting("ArmorLow", true, () -> this.page.getValue() == Aura.Page.Target && this.Players.isOpen())
   );
   private final BooleanSetting Mobs = this.add(new BooleanSetting("Mobs", true, () -> this.page.getValue() == Aura.Page.Target));
   private final BooleanSetting Animals = this.add(new BooleanSetting("Animals", true, () -> this.page.getValue() == Aura.Page.Target));
   private final BooleanSetting Villagers = this.add(new BooleanSetting("Villagers", true, () -> this.page.getValue() == Aura.Page.Target));
   private final BooleanSetting Slimes = this.add(new BooleanSetting("Slimes", true, () -> this.page.getValue() == Aura.Page.Target));
   private final EnumSetting<Aura.TargetMode> targetMode = this.add(
      new EnumSetting("Filter", Aura.TargetMode.DISTANCE, () -> this.page.getValue() == Aura.Page.Target)
   );
   private final EnumSetting<Aura.TargetESP> mode = this.add(new EnumSetting("TargetESP", Aura.TargetESP.Fill, () -> this.page.getValue() == Aura.Page.Render));
   private final SliderSetting animationTime = this.add(
      new SliderSetting("AnimationTime", 200.0, 0.0, 2000.0, 1.0, () -> this.page.getValue() == Aura.Page.Render)
   );
   private final EnumSetting<Easing> ease = this.add(new EnumSetting("Ease", Easing.CubicInOut, () -> this.page.getValue() == Aura.Page.Render));
   private final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 50), () -> this.page.getValue() == Aura.Page.Render));
   private final ColorSetting outlineColor = this.add(
      new ColorSetting("OutlineColor", new Color(255, 255, 255, 50), () -> this.page.getValue() == Aura.Page.Render)
   );
   private final ColorSetting hitColor = this.add(new ColorSetting("HitColor", new Color(255, 255, 255, 150), () -> this.page.getValue() == Aura.Page.Render));
   private final ColorSetting hitOutlineColor = this.add(
      new ColorSetting("HitOutlineColor", new Color(255, 255, 255, 150), () -> this.page.getValue() == Aura.Page.Render)
   );
   private final Animation animation = new Animation();
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true, () -> this.page.getValue() == Aura.Page.Rotate));
   private final BooleanSetting yawStep = this.add(
      new BooleanSetting("YawStep", false, () -> this.rotate.isOpen() && this.page.getValue() == Aura.Page.Rotate).setParent()
   );
   private final BooleanSetting whenElytra = this.add(
      new BooleanSetting("FallFlying", true, () -> this.rotate.isOpen() && this.yawStep.isOpen() && this.page.getValue() == Aura.Page.Rotate)
   );
   private final SliderSetting steps = this.add(
      new SliderSetting("Steps", 0.05, 0.0, 1.0, 0.01, () -> this.page.getValue() == Aura.Page.Rotate && this.yawStep.isOpen())
   );
   private final BooleanSetting checkFov = this.add(
      new BooleanSetting("OnlyLooking", true, () -> this.page.getValue() == Aura.Page.Rotate && this.yawStep.isOpen())
   );
   private final SliderSetting fov = this.add(
      new SliderSetting("Fov", 20.0, 0.0, 360.0, 0.1, () -> this.checkFov.getValue() && this.page.getValue() == Aura.Page.Rotate && this.yawStep.isOpen())
   );
   private final SliderSetting priority = this.add(
      new SliderSetting("Priority", 10, 0, 100, () -> this.page.getValue() == Aura.Page.Rotate && this.yawStep.isOpen())
   );
   private final Timer tick = new Timer();
   public Vec3d directionVec = null;

   public Aura() {
      super("Aura", Module.Category.Combat);
      this.setChinese("杀戮光环");
      INSTANCE = this;
   }

   public static void doRender(MatrixStack stack, float partialTicks, Entity entity, Color color, Color outlineColor, Aura.TargetESP mode) {
      switch (mode) {
         case Fill:
            Render3DUtil.draw3DBox(
               stack,
               ((IEntity)entity)
                  .getDimensions()
                  .getBoxAt(
                     new Vec3d(
                        MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double)partialTicks),
                        MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double)partialTicks),
                        MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double)partialTicks)
                     )
                  )
                  .expand(0.0, 0.1, 0.0),
               color,
               outlineColor,
               false,
               true
            );
            break;
         case Box:
            Render3DUtil.draw3DBox(
               stack,
               ((IEntity)entity)
                  .getDimensions()
                  .getBoxAt(
                     new Vec3d(
                        MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double)partialTicks),
                        MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double)partialTicks),
                        MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double)partialTicks)
                     )
                  )
                  .expand(0.0, 0.1, 0.0),
               color,
               outlineColor,
               true,
               true
            );
            break;
         case Jello:
            JelloUtil.drawJello(stack, entity, color);
            break;
         case ThunderHack:
            Render3DUtil.drawTargetEsp(stack, target, color);
      }
   }

   public static float getAttackCooldownProgressPerTick() {
      return (float)(1.0 / mc.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED) * 20.0);
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      if (target != null) {
         this.doRender(matrixStack, mc.getRenderTickCounter().getTickDelta(true), target, (Aura.TargetESP)this.mode.getValue());
      }
   }

   public void doRender(MatrixStack stack, float partialTicks, Entity entity, Aura.TargetESP mode) {
      switch (mode) {
         case Fill:
            Render3DUtil.draw3DBox(
               stack,
               ((IEntity)entity)
                  .getDimensions()
                  .getBoxAt(
                     new Vec3d(
                        MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double)partialTicks),
                        MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double)partialTicks),
                        MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double)partialTicks)
                     )
                  )
                  .expand(0.0, 0.1, 0.0),
               ColorUtil.fadeColor(
                  this.color.getValue(), this.hitColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), (Easing)this.ease.getValue())
               ),
               ColorUtil.fadeColor(
                  this.outlineColor.getValue(),
                  this.hitOutlineColor.getValue(),
                  this.animation.get(0.0, this.animationTime.getValueInt(), (Easing)this.ease.getValue())
               ),
               false,
               true
            );
            break;
         case Box:
            Render3DUtil.draw3DBox(
               stack,
               ((IEntity)entity)
                  .getDimensions()
                  .getBoxAt(
                     new Vec3d(
                        MathUtil.interpolate(entity.lastRenderX, entity.getX(), (double)partialTicks),
                        MathUtil.interpolate(entity.lastRenderY, entity.getY(), (double)partialTicks),
                        MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), (double)partialTicks)
                     )
                  )
                  .expand(0.0, 0.1, 0.0),
               ColorUtil.fadeColor(
                  this.color.getValue(), this.hitColor.getValue(), this.animation.get(0.0, this.animationTime.getValueInt(), (Easing)this.ease.getValue())
               ),
               ColorUtil.fadeColor(
                  this.outlineColor.getValue(),
                  this.hitOutlineColor.getValue(),
                  this.animation.get(0.0, this.animationTime.getValueInt(), (Easing)this.ease.getValue())
               ),
               true,
               true
            );
            break;
         case Jello:
            JelloUtil.drawJello(stack, entity, this.color.getValue());
            break;
         case ThunderHack:
            Render3DUtil.drawTargetEsp(stack, target, this.color.getValue());
      }
   }

   @Override
   public String getInfo() {
      return target == null ? null : target.getName().getString();
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if ((!this.timing.is(Timing.Pre) || !event.isPost()) && (!this.timing.is(Timing.Post) || !event.isPre())) {
            if (this.weaponOnly.getValue() && !EntityUtil.isHoldingWeapon(mc.player)) {
               target = null;
            } else {
               target = this.getTarget(this.range.getValueFloat());
               if (target == null) {
                  target = this.getTarget(this.targetRange.getValueFloat());
               } else {
                  this.doAura();
               }
            }
         }
      }
   }

   @EventListener
   public void onRotate(RotationEvent event) {
      if (target != null && this.rotate.getValue() && this.shouldYawStep()) {
         this.directionVec = this.getAttackVec(target);
         event.setTarget(this.directionVec, this.steps.getValueFloat(), this.priority.getValueFloat());
      }
   }

   @EventListener
   public void onPacket(PacketEvent.Send event) {
      if (this.reset.getValue()) {
         Packet<?> packet = event.getPacket();
         if (packet instanceof HandSwingC2SPacket
            || packet instanceof PlayerInteractEntityC2SPacket && Criticals.getInteractType((PlayerInteractEntityC2SPacket)packet) == InteractType.ATTACK) {
            this.tick.reset();
         }
      }
   }

   private boolean check() {
      if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
         return false;
      } else {
         int at = (int)(this.tick.getMs() / 50L);
         if (this.cooldownMode.getValue() == Aura.Cooldown.Vanilla) {
            at = ((ILivingEntity)mc.player).getLastAttackedTicks();
         }

         at = (int)(at * Alien.SERVER.getTPSFactor());
         if (!(Math.max(at / getAttackCooldownProgressPerTick(), 0.0F) >= this.cooldown.getValue())) {
            return false;
         } else {
            return target instanceof LivingEntity entity && entity.hurtTime > this.hurtTime.getValue()
               ? false
               : this.whileEating.getValue() || !mc.player.isUsingItem();
         }
      }
   }

   private void doAura() {
      if (this.check()) {
         if (this.rotate.getValue()) {
            Vec3d hitVec = this.getAttackVec(target);
            if (!this.faceVector(hitVec)) {
               return;
            }
         }

         this.animation.to = 1.0;
         this.animation.from = 1.0;
         mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
         mc.player.resetLastAttackedTicks();
         EntityUtil.swingHand(Hand.MAIN_HAND, (SwingSide)this.swingMode.getValue());
         this.tick.reset();
         if (this.rotate.getValue() && !this.shouldYawStep()) {
            Alien.ROTATION.snapBack();
         }
      }
   }

   private Vec3d getAttackVec(Entity entity) {
      return MathUtil.getClosestPointToBox(mc.player.getEyePos(), entity.getBoundingBox());
   }

   private boolean shouldYawStep() {
      return this.whenElytra.getValue() || !mc.player.isFallFlying() && (!ElytraFly.INSTANCE.isOn() || !ElytraFly.INSTANCE.isFallFlying())
         ? this.yawStep.getValue() && !Velocity.INSTANCE.noRotation()
         : false;
   }

   public boolean faceVector(Vec3d directionVec) {
      if (!this.shouldYawStep()) {
         Alien.ROTATION.lookAt(directionVec);
         return true;
      } else {
         this.directionVec = directionVec;
         return Alien.ROTATION.inFov(directionVec, this.fov.getValueFloat()) || !this.checkFov.getValue();
      }
   }

   public Entity getTarget(double range) {
      Entity target = null;
      double distance = range;
      double maxHealth = 36.0;

      for (Entity entity : Alien.THREAD.getEntities()) {
         if (this.isEnemy(entity)) {
            Vec3d hitVec = this.getAttackVec(entity);
            if (!(mc.player.getEyePos().distanceTo(hitVec) > range)
               && (mc.player.canSee(entity) || !(mc.player.getEyePos().distanceTo(hitVec) > this.wallRange.getValue()))
               && CombatUtil.isValid(entity)) {
               if (target == null) {
                  target = entity;
                  distance = mc.player.getEyePos().distanceTo(hitVec);
                  maxHealth = EntityUtil.getHealth(entity);
               } else {
                  if (this.armorLow.getValue() && entity instanceof PlayerEntity && EntityUtil.isArmorLow((PlayerEntity)entity, 10)) {
                     target = entity;
                     break;
                  }

                  if (this.targetMode.getValue() == Aura.TargetMode.HEALTH && EntityUtil.getHealth(entity) < maxHealth) {
                     target = entity;
                     maxHealth = EntityUtil.getHealth(entity);
                  } else if (this.targetMode.getValue() == Aura.TargetMode.DISTANCE && mc.player.getEyePos().distanceTo(hitVec) < distance) {
                     target = entity;
                     distance = mc.player.getEyePos().distanceTo(hitVec);
                  }
               }
            }
         }
      }

      return target;
   }

   private boolean isEnemy(Entity entity) {
      if (entity instanceof SlimeEntity) {
         return this.Slimes.getValue();
      } else if (entity instanceof PlayerEntity) {
         return this.Players.getValue();
      } else if (entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity) {
         return this.Villagers.getValue();
      } else if (entity instanceof AnimalEntity) {
         return this.Animals.getValue();
      } else {
         return entity instanceof MobEntity ? this.Mobs.getValue() : false;
      }
   }

   public static enum Cooldown {
      Vanilla,
      Delay;
   }

   public static enum Mode {
      Mace,
      Axe,
      Sword;
   }

   public static enum Page {
      General,
      Rotate,
      Target,
      Render;
   }

   public static enum TargetESP {
      Fill,
      Box,
      Jello,
      ThunderHack,
      None;
   }

   private static enum TargetMode {
      DISTANCE,
      HEALTH;
   }
}
