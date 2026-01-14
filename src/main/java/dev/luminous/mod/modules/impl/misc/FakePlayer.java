package dev.luminous.mod.modules.impl.misc;

import com.mojang.authlib.GameProfile;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.DamageUtils;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.asm.accessors.ILivingEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.combat.AutoAnchor;
import dev.luminous.mod.modules.impl.combat.AutoCrystal;
import dev.luminous.mod.modules.impl.combat.Criticals;
import dev.luminous.mod.modules.impl.combat.Criticals.Mode;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractType;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class FakePlayer extends Module {
   public static FakePlayer INSTANCE;
   public static FakePlayer.FakePlayerEntity fakePlayer;
   final StringSetting name = this.add(new StringSetting("Name", "FakePlayer"));
   private final BooleanSetting damage = this.add(new BooleanSetting("Damage", true));
   private final BooleanSetting autoTotem = this.add(new BooleanSetting("AutoTotem", true));
   public final BooleanSetting record = this.add(new BooleanSetting("Record", false));
   public final BooleanSetting play = this.add(new BooleanSetting("Play", false));
   final List<FakePlayer.PlayerState> positions = new ArrayList();
   int movementTick;
   boolean lastRecordValue = false;

   public FakePlayer() {
      super("FakePlayer", Module.Category.Misc);
      this.setChinese("假人");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      return this.name.getValue();
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         fakePlayer = new FakePlayer.FakePlayerEntity(mc.player, this.name.getValue());
         mc.world.addEntity(fakePlayer);
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (fakePlayer != null && fakePlayer.clientWorld == mc.world) {
         if (this.autoTotem.getValue()) {
            if (fakePlayer.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
               fakePlayer.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }

            if (fakePlayer.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
               fakePlayer.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }
         }

         if (this.record.getValue() != this.lastRecordValue && this.record.getValue()) {
            this.positions.clear();
         }

         this.lastRecordValue = this.record.getValue();
         if (this.record.getValue()) {
            this.positions
               .add(
                  new FakePlayer.PlayerState(
                     mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch()
                  )
               );
         }

         if (this.play.getValue() && !this.positions.isEmpty()) {
            this.movementTick++;
            if (this.movementTick >= this.positions.size()) {
               this.movementTick = 0;
            }

            FakePlayer.PlayerState p = (FakePlayer.PlayerState)this.positions.get(this.movementTick);
            fakePlayer.setYaw(p.yaw);
            fakePlayer.setPitch(p.pitch);
            fakePlayer.setHeadYaw(p.yaw);
            fakePlayer.updateTrackedPosition(p.x, p.y, p.z);
            fakePlayer.updateTrackedPositionAndAngles(p.x, p.y, p.z, p.yaw, p.pitch, 3);
         }
      } else {
         this.disable();
      }
   }

   @Override
   public void onDisable() {
      if (fakePlayer != null) {
         fakePlayer.kill();
         fakePlayer.setRemoved(RemovalReason.KILLED);
         fakePlayer.onRemoved();
         fakePlayer = null;
      }
   }

   @EventListener
   public void onAttack(PacketEvent.Send event) {
      if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet
         && Criticals.getInteractType(packet) == InteractType.ATTACK
         && Criticals.getEntity(packet) == fakePlayer) {
         mc.world
            .playSound(
               mc.player,
               fakePlayer.getX(),
               fakePlayer.getY(),
               fakePlayer.getZ(),
               SoundEvents.ENTITY_PLAYER_HURT,
               SoundCategory.PLAYERS,
               1.0F,
               1.0F
            );
         float damage = DamageUtils.getAttackDamage(mc.player, fakePlayer);
         if ((
               mc.player.fallDistance > 0.0F
                  || Criticals.INSTANCE.isOn()
                     && !Criticals.INSTANCE.mode.is(Criticals.Mode.Ground)
                     && (mc.player.isOnGround() || !Criticals.INSTANCE.onlyGround.getValue())
            )
            && (!mc.player.isOnGround() || Criticals.INSTANCE.isOn() && !Criticals.INSTANCE.mode.is(Criticals.Mode.Ground))
            && !mc.player.isClimbing()
            && !mc.player.isTouchingWater()
            && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
            && !mc.player.hasVehicle()) {
            mc.world
               .playSound(
                  mc.player,
                  fakePlayer.getX(),
                  fakePlayer.getY(),
                  fakePlayer.getZ(),
                  SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                  SoundCategory.PLAYERS,
                  1.0F,
                  1.0F
               );
            mc.player.addCritParticles(fakePlayer);
         }

         if (fakePlayer.hurtTime <= 0) {
            fakePlayer.onDamaged(mc.world.getDamageSources().generic());
            if (fakePlayer.getAbsorptionAmount() >= damage) {
               fakePlayer.setAbsorptionAmount(fakePlayer.getAbsorptionAmount() - damage);
            } else {
               float damage2 = damage - fakePlayer.getAbsorptionAmount();
               fakePlayer.setAbsorptionAmount(0.0F);
               fakePlayer.setHealth(fakePlayer.getHealth() - damage2);
            }

            if (fakePlayer.isDead()) {
               Alien.POP.onTotemPop(fakePlayer);
               if (fakePlayer.tryUseTotem(mc.world.getDamageSources().generic())) {
                  fakePlayer.setHealth(10.0F);
                  new EntityStatusS2CPacket(fakePlayer, (byte)35).apply(mc.getNetworkHandler());
               }
            }
         }
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (this.damage.getValue() && fakePlayer != null && fakePlayer.hurtTime <= 0 && event.getPacket() instanceof ExplosionS2CPacket explosion) {
         if (Math.sqrt(new Vec3d(explosion.getX(), explosion.getY(), explosion.getZ()).squaredDistanceTo(fakePlayer.getPos()))
            > 10.0) {
            return;
         }

         float damage;
         if (BlockUtil.getBlock(new BlockPosX(explosion.getX(), explosion.getY(), explosion.getZ())) == Blocks.RESPAWN_ANCHOR) {
            damage = (float)AutoAnchor.INSTANCE
               .getAnchorDamage(new BlockPosX(explosion.getX(), explosion.getY(), explosion.getZ()), fakePlayer, fakePlayer);
         } else {
            damage = AutoCrystal.INSTANCE
               .calculateDamage(new Vec3d(explosion.getX(), explosion.getY(), explosion.getZ()), fakePlayer, fakePlayer);
         }

         fakePlayer.onDamaged(mc.world.getDamageSources().generic());
         if (fakePlayer.getAbsorptionAmount() >= damage) {
            fakePlayer.setAbsorptionAmount(fakePlayer.getAbsorptionAmount() - damage);
         } else {
            float damage2 = damage - fakePlayer.getAbsorptionAmount();
            fakePlayer.setAbsorptionAmount(0.0F);
            fakePlayer.setHealth(fakePlayer.getHealth() - damage2);
         }

         if (fakePlayer.isDead()) {
            Alien.POP.onTotemPop(fakePlayer);
            if (fakePlayer.tryUseTotem(mc.world.getDamageSources().generic())) {
               fakePlayer.setHealth(10.0F);
               new EntityStatusS2CPacket(fakePlayer, (byte)35).apply(mc.getNetworkHandler());
            }
         }
      }
   }

   public static class FakePlayerEntity extends OtherClientPlayerEntity {
      private final boolean onGround;

      public FakePlayerEntity(PlayerEntity player, String name) {
         super(Wrapper.mc.world, new GameProfile(UUID.fromString("66666666-6666-6666-6666-666666666666"), name));
         this.copyPositionAndRotation(player);
         this.prevX = player.prevX;
         this.prevZ = player.prevZ;
         this.prevY = player.prevY;
         this.bodyYaw = player.bodyYaw;
         this.headYaw = player.headYaw;
         this.handSwingProgress = player.handSwingProgress;
         this.handSwingTicks = player.handSwingTicks;
         this.limbAnimator.setSpeed(player.limbAnimator.getSpeed());
         this.limbAnimator.pos = player.limbAnimator.getPos();
         ((ILivingEntity)this).setLeaningPitch(((ILivingEntity)player).getLeaningPitch());
         ((ILivingEntity)this).setLastLeaningPitch(((ILivingEntity)player).getLeaningPitch());
         this.touchingWater = player.isTouchingWater();
         this.setSneaking(player.isSneaking());
         this.setPose(player.getPose());
         this.setFlag(7, player.isFallFlying());
         this.onGround = player.isOnGround();
         this.setOnGround(this.onGround);
         this.getInventory().clone(player.getInventory());
         this.setAbsorptionAmountUnclamped(player.getAbsorptionAmount());
         this.setHealth(player.getHealth());
         this.setBoundingBox(player.getBoundingBox());
      }

      public boolean isOnGround() {
         return this.onGround;
      }

      public boolean isSpectator() {
         return false;
      }

      public boolean isCreative() {
         return false;
      }
   }

   private record PlayerState(double x, double y, double z, float yaw, float pitch) {
   }
}
