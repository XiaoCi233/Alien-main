package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.*;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.player.EntityUtil;
import dev.luminous.api.utils.player.InventoryUtil;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.asm.accessors.IFireworkRocketEntity;
import dev.luminous.asm.accessors.ILivingEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ElytraFly extends Module {
   public static ElytraFly INSTANCE;
   public final EnumSetting<ElytraFly.Mode> mode = this.add(new EnumSetting("Mode", ElytraFly.Mode.Control));
   public final BooleanSetting infiniteDura = this.add(new BooleanSetting("InfiniteDura", false));
   public final BooleanSetting packet = this.add(new BooleanSetting("Packet", false).setParent());
   private final SliderSetting packetDelay = this.add(new SliderSetting("PacketDelay", 0.0, 0.0, 20.0, 1.0, this.packet::isOpen));
   private final BooleanSetting setFlag = this.add(new BooleanSetting("SetFlag", false, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
   private final BooleanSetting firework = this.add(new BooleanSetting("Firework", false).setParent());
   public final BindSetting fireWork = this.add(new BindSetting("FireWorkBind", -1, this.firework::isOpen));
   public final BooleanSetting packetInteract = this.add(new BooleanSetting("PacketInteract", true, this.firework::isOpen));
   public final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true, this.firework::isOpen));
   public final BooleanSetting onlyOne = this.add(new BooleanSetting("OnlyOne", true, this.firework::isOpen));
   private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true, this.firework::isOpen));
   public final BooleanSetting autoJump = this.add(new BooleanSetting("AutoJump", true, () -> this.mode.is(ElytraFly.Mode.Bounce)));
   public final SliderSetting upPitch = this.add(new SliderSetting("UpPitch", 0.0, 0.0, 90.0, () -> this.mode.getValue() == ElytraFly.Mode.Control));
   public final SliderSetting upFactor = this.add(new SliderSetting("UpFactor", 1.0, 0.0, 10.0, () -> this.mode.getValue() == ElytraFly.Mode.Control));
   public final SliderSetting downFactor = this.add(new SliderSetting("FallSpeed", 1.0, 0.0, 10.0, () -> this.mode.getValue() == ElytraFly.Mode.Control));
   public final SliderSetting speed = this.add(new SliderSetting("Speed", 1.0, 0.1F, 10.0, () -> this.mode.getValue() == ElytraFly.Mode.Control));
   public final BooleanSetting speedLimit = this.add(new BooleanSetting("SpeedLimit", true, () -> this.mode.getValue() == ElytraFly.Mode.Control));
   public final SliderSetting maxSpeed = this.add(
      new SliderSetting("MaxSpeed", 2.5, 0.1F, 10.0, () -> this.speedLimit.getValue() && this.mode.getValue() == ElytraFly.Mode.Control)
   );
   public final BooleanSetting noDrag = this.add(new BooleanSetting("NoDrag", false, () -> this.mode.getValue() == ElytraFly.Mode.Control));
   public final Timer fireworkTimer = new Timer();
   private final BooleanSetting autoStop = this.add(new BooleanSetting("AutoStop", true));
   private final BooleanSetting sprint = this.add(new BooleanSetting("Sprint", true, () -> this.mode.is(ElytraFly.Mode.Bounce)));
   private final SliderSetting pitch = this.add(new SliderSetting("Pitch", 88.0, -90.0, 90.0, 0.1, () -> this.mode.is(ElytraFly.Mode.Bounce)));
   private final BooleanSetting instantFly = this.add(new BooleanSetting("AutoStart", true, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
   private final BooleanSetting checkSpeed = this.add(new BooleanSetting("CheckSpeed", false, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
   public final SliderSetting minSpeed = this.add(new SliderSetting("MinSpeed", 70.0, 0.1, 200.0, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 1000.0, 0.0, 20000.0, 50.0, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
   private final SliderSetting timeout = this.add(new SliderSetting("Timeout", 0.0, 0.1, 1.0, 0.1, () -> !this.mode.is(ElytraFly.Mode.Bounce)));
   private final SliderSetting sneakDownSpeed = this.add(new SliderSetting("DownSpeed", 1.0, 0.1, 10.0, () -> this.mode.getValue() == ElytraFly.Mode.Control));
   private final SliderSetting boost = this.add(new SliderSetting("Boost", 1.0, 0.1, 4.0, () -> this.mode.getValue() == ElytraFly.Mode.Boost));
   private final BooleanSetting freeze = this.add(new BooleanSetting("Freeze", false, () -> this.mode.is(ElytraFly.Mode.Rotation)));
   private final BooleanSetting motionStop = this.add(new BooleanSetting("MotionStop", false, () -> this.mode.is(ElytraFly.Mode.Rotation)));
   private final SliderSetting infiniteMaxSpeed = this.add(
      new SliderSetting("InfiniteMaxSpeed", 150.0, 50.0, 170.0, () -> this.mode.getValue() == ElytraFly.Mode.Pitch)
   );
   private final SliderSetting infiniteMinSpeed = this.add(
      new SliderSetting("InfiniteMinSpeed", 25.0, 10.0, 70.0, () -> this.mode.getValue() == ElytraFly.Mode.Pitch)
   );
   private final SliderSetting infiniteMaxHeight = this.add(
      new SliderSetting("InfiniteMaxHeight", 200, -50, 360, () -> this.mode.getValue() == ElytraFly.Mode.Pitch)
   );
   public final BooleanSetting releaseSneak = this.add(new BooleanSetting("ReleaseSneak", false));
   private final Timer instantFlyTimer = new Timer();
   boolean prev;
   float prePitch;
   private boolean hasElytra = false;
   float yaw = 0.0F;
   float rotationPitch = 0.0F;
   boolean flying = false;
   int packetDelayInt = 0;
   private boolean down;
   private float lastInfinitePitch;
   private float infinitePitch;

   public ElytraFly() {
      super("ElytraFly", Module.Category.Movement);
      this.setChinese("鞘翅飞行");
      INSTANCE = this;
      Alien.EVENT_BUS.subscribe(new ElytraFly.FireWorkTweak());
   }

   public void off() {
      if (!this.inventory.getValue() || EntityUtil.inInventory()) {
         if (this.onlyOne.getValue()) {
            for (Entity entity : Alien.THREAD.getEntities()) {
               if (entity instanceof FireworkRocketEntity fireworkRocketEntity && ((IFireworkRocketEntity)fireworkRocketEntity).getShooter() == mc.player) {
                  return;
               }
            }
         }

         INSTANCE.fireworkTimer.reset();
         if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
            if (this.packetInteract.getValue()) {
               sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
            } else {
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
         } else {
            int firework;
            if (this.inventory.getValue() && (firework = InventoryUtil.findItemInventorySlot(Items.FIREWORK_ROCKET)) != -1) {
               InventoryUtil.inventorySwap(firework, mc.player.getInventory().selectedSlot);
               if (this.packetInteract.getValue()) {
                  sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
               } else {
                  mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               }

               InventoryUtil.inventorySwap(firework, mc.player.getInventory().selectedSlot);
               EntityUtil.syncInventory();
            } else if ((firework = InventoryUtil.findItem(Items.FIREWORK_ROCKET)) != -1) {
               int old = mc.player.getInventory().selectedSlot;
               InventoryUtil.switchToSlot(firework);
               if (this.packetInteract.getValue()) {
                  sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch()));
               } else {
                  mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               }

               InventoryUtil.switchToSlot(old);
            }
         }
      }
   }

   public static boolean recastElytra(ClientPlayerEntity player) {
      if (checkConditions(player) && ignoreGround(player)) {
         player.networkHandler
            .sendPacket(new ClientCommandC2SPacket(player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
         if (INSTANCE.setFlag.getValue()) {
            mc.player.startFallFlying();
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean checkConditions(ClientPlayerEntity player) {
      ItemStack itemStack = player.getEquippedStack(EquipmentSlot.CHEST);
      return !player.getAbilities().flying
         && !player.hasVehicle()
         && !player.isClimbing()
         && itemStack.isOf(Items.ELYTRA)
         && ElytraItem.isUsable(itemStack);
   }

   private static boolean ignoreGround(ClientPlayerEntity player) {
      if (!player.isTouchingWater() && !player.hasStatusEffect(StatusEffects.LEVITATION)) {
         ItemStack itemStack = player.getEquippedStack(EquipmentSlot.CHEST);
         if (itemStack.isOf(Items.ELYTRA) && ElytraItem.isUsable(itemStack)) {
            player.startFallFlying();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public String getInfo() {
      return ((ElytraFly.Mode)this.mode.getValue()).name();
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         this.hasElytra = false;
         this.yaw = mc.player.getYaw();
         this.rotationPitch = mc.player.getPitch();
      }
   }

   @Override
   public void onDisable() {
      if (!nullCheck()) {
         if (this.releaseSneak.getValue()) {
            mc.getNetworkHandler()
               .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
         }
      }
   }

   private void boost() {
      if (this.hasElytra) {
         if (!this.isFallFlying()) {
            return;
         }

         float yaw = (float)Math.toRadians(mc.player.getYaw());
         if (mc.options.forwardKey.isPressed()) {
            mc.player.addVelocity(-MathHelper.sin(yaw) * this.boost.getValueFloat() / 10.0F, 0.0, MathHelper.cos(yaw) * this.boost.getValueFloat() / 10.0F);
         }
      }
   }

   @EventListener(priority = -9999)
   public void onRotation(UpdateRotateEvent event) {
      if (!nullCheck()) {
         if (this.mode.is(ElytraFly.Mode.Rotation)) {
            if (this.isFallFlying()) {
               if (MovementUtil.isMoving()) {
                  if (mc.options.jumpKey.isPressed()) {
                     this.rotationPitch = -45.0F;
                  } else if (mc.options.sneakKey.isPressed()) {
                     this.rotationPitch = 45.0F;
                  } else {
                     this.rotationPitch = -1.9F;
                     if (this.motionStop.getValue()) {
                        this.setY(0.0);
                     }
                  }
               } else if (mc.options.jumpKey.isPressed()) {
                  this.rotationPitch = -89.0F;
               } else if (mc.options.sneakKey.isPressed()) {
                  this.rotationPitch = 89.0F;
               } else if (this.motionStop.getValue()) {
                  this.setY(0.0);
               }

               if (mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed()) {
                  this.yaw = Sprint.getSprintYaw(mc.player.getYaw());
               } else if (this.motionStop.getValue()) {
                  this.setX(0.0);
                  this.setZ(0.0);
               }

               event.setYaw(this.yaw);
               event.setPitch(this.rotationPitch);
            }
         } else if (this.mode.is(ElytraFly.Mode.Pitch)) {
            if (this.isFallFlying()) {
               event.setPitch(this.infinitePitch);
            }
         } else if (this.mode.is(ElytraFly.Mode.Bounce) && this.isFallFlying()) {
            event.setPitch(this.pitch.getValueFloat());
         }
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.getInfinitePitch();
      this.flying = false;
      if (this.packet.getValue()) {
         this.hasElytra = InventoryUtil.findItemInventorySlot(Items.ELYTRA) != -1;
      } else {
         this.hasElytra = false;

         for (ItemStack is : mc.player.getArmorItems()) {
            if (is.getItem() instanceof ElytraItem) {
               this.hasElytra = true;
               break;
            }
         }

         if (this.infiniteDura.getValue() && !mc.player.isOnGround() && this.hasElytra) {
            this.flying = true;
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
            mc.getNetworkHandler()
               .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            if (this.setFlag.getValue()) {
               mc.player.startFallFlying();
            }
         }

         if (this.mode.is(ElytraFly.Mode.Bounce)) {
            ((ILivingEntity)mc.player).setLastJumpCooldown(0);
            return;
         }
      }

      double x = mc.player.getX() - mc.player.prevX;
      double y = mc.player.getY() - mc.player.prevY;
      double z = mc.player.getZ() - mc.player.prevZ;
      double dist = Math.sqrt(x * x + z * z + y * y) / 1000.0;
      double div = 1.388888888888889E-5;
      float timer = Alien.TIMER.get();
      double speed = dist / div * timer;
      if (this.mode.getValue() == ElytraFly.Mode.Boost) {
         this.boost();
      }

      if (this.packet.getValue()) {
         if (!mc.player.isOnGround()) {
            this.packetDelayInt++;
            if (!(this.packetDelayInt <= this.packetDelay.getValue())) {
               int elytra = InventoryUtil.findItem(Items.ELYTRA);
               if (elytra != -1) {
                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, elytra, SlotActionType.SWAP, mc.player);
                  mc.getNetworkHandler()
                     .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                  mc.player.startFallFlying();
                  if ((!this.checkSpeed.getValue() || speed <= this.minSpeed.getValue())
                     && this.firework.getValue()
                     && this.fireworkTimer.passed(this.delay.getValueInt())
                     && (MovementUtil.isMoving() || this.mode.is(ElytraFly.Mode.Rotation) && mc.options.jumpKey.isPressed())
                     && (!mc.player.isUsingItem() || !this.usingPause.getValue())
                     && this.isFallFlying()) {
                     this.off();
                     this.fireworkTimer.reset();
                  }

                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, elytra, SlotActionType.SWAP, mc.player);
                  this.packetDelayInt = 0;
               } else {
                  elytra = InventoryUtil.findItemInventorySlot(Items.ELYTRA);
                  if (elytra != -1) {
                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytra, 0, SlotActionType.PICKUP, mc.player);
                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                     mc.getNetworkHandler()
                        .sendPacket(
                           new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING)
                        );
                     mc.player.startFallFlying();
                     if ((!this.checkSpeed.getValue() || speed <= this.minSpeed.getValue())
                        && this.firework.getValue()
                        && this.fireworkTimer.passed(this.delay.getValueInt())
                        && (MovementUtil.isMoving() || this.mode.is(ElytraFly.Mode.Rotation) && mc.options.jumpKey.isPressed())
                        && (!mc.player.isUsingItem() || !this.usingPause.getValue())
                        && this.isFallFlying()) {
                        this.off();
                        this.fireworkTimer.reset();
                     }

                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                     mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytra, 0, SlotActionType.PICKUP, mc.player);
                     this.packetDelayInt = 0;
                  }
               }
            }
         }
      } else {
         if ((!this.checkSpeed.getValue() || speed <= this.minSpeed.getValue())
            && this.firework.getValue()
            && this.fireworkTimer.passed(this.delay.getValueInt())
            && (MovementUtil.isMoving() || this.mode.is(ElytraFly.Mode.Rotation) && mc.options.jumpKey.isPressed())
            && (!mc.player.isUsingItem() || !this.usingPause.getValue())
            && this.isFallFlying()) {
            this.off();
            this.fireworkTimer.reset();
         }

         if (!this.isFallFlying() && this.hasElytra) {
            this.fireworkTimer.setMs(99999999L);
            if (!mc.player.isOnGround() && this.instantFly.getValue() && mc.player.getVelocity().getY() < 0.0 && !this.infiniteDura.getValue()) {
               if (!this.instantFlyTimer.passed((long)(1000.0 * this.timeout.getValue()))) {
                  return;
               }

               this.instantFlyTimer.reset();
               mc.getNetworkHandler()
                  .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               if (this.setFlag.getValue()) {
                  mc.player.startFallFlying();
               }
            }
         }
      }
   }

   protected final Vec3d getRotationVector(float pitch, float yaw) {
      float f = pitch * (float) (Math.PI / 180.0);
      float g = -yaw * (float) (Math.PI / 180.0);
      float h = MathHelper.cos(g);
      float i = MathHelper.sin(g);
      float j = MathHelper.cos(f);
      float k = MathHelper.sin(f);
      return new Vec3d(i * j, -k, h * j);
   }

   public Vec3d getRotationVec(float tickDelta) {
      return this.getRotationVector(-this.upPitch.getValueFloat(), mc.player.getYaw(tickDelta));
   }

   @EventListener
   private void onPlayerMove(MoveEvent event) {
      if (this.autoStop.getValue() && this.isFallFlying()) {
         int chunkX = (int)(mc.player.getX() / 16.0);
         int chunkZ = (int)(mc.player.getZ() / 16.0);
         if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
            event.cancel();
         }
      }
   }

   @EventListener
   private void onTick(ClientTickEvent event) {
      if (!nullCheck()) {
         if (this.mode.is(ElytraFly.Mode.Bounce) && this.hasElytra) {
            if (this.autoJump.getValue()) {
               mc.options.jumpKey.setPressed(true);
            }

            if (event.isPost()) {
               if (!this.isFallFlying()) {
                  mc.getNetworkHandler()
                     .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
               }

               if (checkConditions(mc.player) && !this.sprint.getValue()) {
                  if (this.isFallFlying()) {
                     mc.player.setSprinting(mc.player.isOnGround());
                  } else {
                     mc.player.setSprinting(true);
                  }
               }
            } else if (checkConditions(mc.player) && this.sprint.getValue()) {
               mc.player.setSprinting(true);
            }
         }
      }
   }

   @EventListener
   private void onPacketSend(PacketEvent.Send event) {
      if (!nullCheck()) {
         if (this.mode.is(ElytraFly.Mode.Bounce)
            && this.hasElytra
            && event.getPacket() instanceof ClientCommandC2SPacket
            && ((ClientCommandC2SPacket)event.getPacket())
               .getMode()
               .equals(net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING)
            && !this.sprint.getValue()) {
            mc.player.setSprinting(true);
         }
      }
   }

   @EventListener
   private void onPacketReceive(PacketEvent.Receive event) {
      if (!nullCheck()) {
         if (this.mode.is(ElytraFly.Mode.Bounce) && this.hasElytra && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            mc.player.stopFallFlying();
         }
      }
   }

   @EventListener
   public void travel(TravelEvent event) {
      if (!nullCheck()) {
         if (!AntiCheat.INSTANCE.movementSync()) {
            if (this.mode.is(ElytraFly.Mode.Bounce) && this.hasElytra) {
               if (event.isPre()) {
                  this.prev = true;
                  this.prePitch = mc.player.getPitch();
                  mc.player.setPitch(this.pitch.getValueFloat());
               } else if (this.prev) {
                  this.prev = false;
                  mc.player.setPitch(this.prePitch);
               }
            } else if (this.mode.is(ElytraFly.Mode.Pitch) && this.isFallFlying()) {
               if (event.isPre()) {
                  this.prev = true;
                  this.prePitch = mc.player.getPitch();
                  mc.player.setPitch(this.lastInfinitePitch);
               } else if (this.prev) {
                  this.prev = false;
                  mc.player.setPitch(this.prePitch);
               }
            }
         }
      }
   }

   @EventListener
   public void onMove(TravelEvent event) {
      if (!nullCheck() && this.hasElytra && this.isFallFlying() && !event.isPost()) {
         if ((this.mode.is(ElytraFly.Mode.Freeze) || this.mode.is(ElytraFly.Mode.Rotation) && this.freeze.getValue())
            && !MovementUtil.isMoving()
            && !mc.options.jumpKey.isPressed()
            && !mc.options.sneakKey.isPressed()) {
            event.cancel();
         } else {
            if (this.mode.getValue() == ElytraFly.Mode.Control) {
               if (this.firework.getValue()) {
                  if (mc.options.sneakKey.isPressed() && mc.player.input.jumping) {
                     this.setY(0.0);
                  } else if (mc.options.sneakKey.isPressed()) {
                     this.setY(-this.sneakDownSpeed.getValue());
                  } else if (mc.player.input.jumping) {
                     this.setY(this.upFactor.getValue());
                  } else {
                     this.setY(-3.0E-11 * this.downFactor.getValue());
                  }

                  double[] dir = MovementUtil.directionSpeed(this.speed.getValue());
                  this.setX(dir[0]);
                  this.setZ(dir[1]);
               } else {
                  Vec3d lookVec = this.getRotationVec(mc.getRenderTickCounter().getTickDelta(true));
                  double lookDist = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
                  double motionDist = Math.sqrt(this.getX() * this.getX() + this.getZ() * this.getZ());
                  if (mc.player.input.sneaking) {
                     this.setY(-this.sneakDownSpeed.getValue());
                  } else if (!mc.player.input.jumping) {
                     this.setY(-3.0E-11 * this.downFactor.getValue());
                  }

                  if (mc.player.input.jumping) {
                     if (motionDist > this.upFactor.getValue() / this.upFactor.getMax()) {
                        double rawUpSpeed = motionDist * 0.01325;
                        this.setY(this.getY() + rawUpSpeed * 3.2);
                        this.setX(this.getX() - lookVec.x * rawUpSpeed / lookDist);
                        this.setZ(this.getZ() - lookVec.z * rawUpSpeed / lookDist);
                     } else {
                        double[] dir = MovementUtil.directionSpeed(this.speed.getValue());
                        this.setX(dir[0]);
                        this.setZ(dir[1]);
                     }
                  }

                  if (lookDist > 0.0) {
                     this.setX(this.getX() + (lookVec.x / lookDist * motionDist - this.getX()) * 0.1);
                     this.setZ(this.getZ() + (lookVec.z / lookDist * motionDist - this.getZ()) * 0.1);
                  }

                  if (!mc.player.input.jumping) {
                     double[] dir = MovementUtil.directionSpeed(this.speed.getValue());
                     this.setX(dir[0]);
                     this.setZ(dir[1]);
                  }

                  if (!this.noDrag.getValue()) {
                     this.setY(this.getY() * 0.99F);
                     this.setX(this.getX() * 0.98F);
                     this.setZ(this.getZ() * 0.99F);
                  }

                  double finalDist = Math.sqrt(this.getX() * this.getX() + this.getZ() * this.getZ());
                  if (this.speedLimit.getValue() && finalDist > this.maxSpeed.getValue()) {
                     this.setX(this.getX() * this.maxSpeed.getValue() / finalDist);
                     this.setZ(this.getZ() * this.maxSpeed.getValue() / finalDist);
                  }

                  event.cancel();
                  mc.player.move(MovementType.SELF, mc.player.getVelocity());
               }
            }
         }
      }
   }

   private double getX() {
      return MovementUtil.getMotionX();
   }

   private void setX(double f) {
      MovementUtil.setMotionX(f);
   }

   private double getY() {
      return MovementUtil.getMotionY();
   }

   private void setY(double f) {
      MovementUtil.setMotionY(f);
   }

   private double getZ() {
      return MovementUtil.getMotionZ();
   }

   private void setZ(double f) {
      MovementUtil.setMotionZ(f);
   }

   private void getInfinitePitch() {
      this.lastInfinitePitch = this.infinitePitch;
      double currentPlayerSpeed = Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ);
      if (mc.player.getY() < this.infiniteMaxHeight.getValue()) {
         if (currentPlayerSpeed * 72.0 < this.infiniteMinSpeed.getValue() && !this.down) {
            this.down = true;
         }

         if (currentPlayerSpeed * 72.0 > this.infiniteMaxSpeed.getValue() && this.down) {
            this.down = false;
         }
      } else {
         this.down = true;
      }

      if (this.down) {
         this.infinitePitch += 3.0F;
      } else {
         this.infinitePitch -= 3.0F;
      }

      this.infinitePitch = MathUtil.clamp(this.infinitePitch, -40.0F, 40.0F);
   }

   public boolean isFallFlying() {
      return mc.player.isFallFlying() || this.packet.getValue() && this.hasElytra && !mc.player.isOnGround() || this.flying;
   }

   public class FireWorkTweak {
      boolean press;

      @EventListener
      public void onTick(ClientTickEvent event) {
         if (!Module.nullCheck() && !event.isPost()) {
            if (!ElytraFly.this.inventory.getValue() || EntityUtil.inInventory()) {
               if (Wrapper.mc.currentScreen == null) {
                  if (ElytraFly.this.fireWork.isPressed()) {
                     if (!this.press
                        && ElytraFly.this.fireworkTimer.passed(ElytraFly.this.delay.getValueInt())
                        && (!Wrapper.mc.player.isUsingItem() || !ElytraFly.this.usingPause.getValue())
                        && ElytraFly.this.isFallFlying()) {
                        ElytraFly.this.off();
                        ElytraFly.this.fireworkTimer.reset();
                     }

                     this.press = true;
                  } else {
                     this.press = false;
                  }
               } else {
                  this.press = false;
               }
            }
         }
      }
   }

   public static enum Mode {
      Control,
      Boost,
      Bounce,
      Freeze,
      None,
      Rotation,
      Pitch;
   }
}
