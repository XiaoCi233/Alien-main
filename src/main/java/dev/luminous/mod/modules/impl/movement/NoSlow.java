package dev.luminous.mod.modules.impl.movement;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.InteractItemEvent;
import dev.luminous.api.events.impl.KeyboardInputEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.PacketEvent.Sent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.player.MovementUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class NoSlow extends Module {
   public static NoSlow INSTANCE;
   final Queue<ClickSlotC2SPacket> storedClicks = new LinkedList();
   final AtomicBoolean pause = new AtomicBoolean();
   private final EnumSetting<NoSlow.Mode> mode = this.add(new EnumSetting("Mode", NoSlow.Mode.Vanilla));
   private final BooleanSetting soulSand = this.add(new BooleanSetting("SoulSand", true));
   private final BooleanSetting sneak = this.add(new BooleanSetting("Sneak", false));
   private final BooleanSetting climb = this.add(new BooleanSetting("Climb", false));
   private final BooleanSetting gui = this.add(new BooleanSetting("Gui", true));
   private final BooleanSetting allowSneak = this.add(new BooleanSetting("AllowSneak", false, this.gui::getValue));
   private final EnumSetting<NoSlow.Bypass> clickBypass = this.add(new EnumSetting("GuiMoveBypass", NoSlow.Bypass.None));
   boolean using = false;
   int delay = 0;

   public NoSlow() {
      super("NoSlow", Module.Category.Movement);
      this.setChinese("无减速");
      INSTANCE = this;
   }

   private static float getMovementMultiplier(boolean positive, boolean negative) {
      if (positive == negative) {
         return 0.0F;
      } else {
         return positive ? 1.0F : -1.0F;
      }
   }

   @Override
   public String getInfo() {
      return ((NoSlow.Mode)this.mode.getValue()).name();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.using = mc.player.isUsingItem();
      this.delay--;
      if (this.using) {
         this.delay = 2;
      }

      if (this.using && !mc.player.isRiding() && !mc.player.isFallFlying()) {
         switch ((NoSlow.Mode)this.mode.getValue()) {
            case NCP:
               mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
               break;
            case Grim:
               if (mc.player.getActiveHand() == Hand.OFF_HAND) {
                  sendSequencedPacket(
                     id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                  );
               } else {
                  sendSequencedPacket(
                     id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                  );
               }
               break;
            case GrimPacket:
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 1, 0, SlotActionType.PICKUP, mc.player);
               if (mc.player.getActiveHand() == Hand.OFF_HAND) {
                  sendSequencedPacket(
                     id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                  );
               } else {
                  sendSequencedPacket(
                     id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, Alien.ROTATION.getLastYaw(), Alien.ROTATION.getLastPitch())
                  );
               }
         }
      }

      if (this.gui.getValue() && !(mc.currentScreen instanceof ChatScreen)) {
         for (KeyBinding k : new KeyBinding[]{mc.options.backKey, mc.options.leftKey, mc.options.rightKey}) {
            k.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(k.getBoundKeyTranslationKey()).getCode()));
         }

         mc.options
            .jumpKey
            .setPressed(
               ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.mode.is(ElytraFly.Mode.Bounce) && ElytraFly.INSTANCE.autoJump.getValue()
                  || InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.jumpKey.getBoundKeyTranslationKey()).getCode())
            );
         mc.options
            .forwardKey
            .setPressed(
               AutoWalk.INSTANCE.forward()
                  || InputUtil.isKeyPressed(
                     mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.forwardKey.getBoundKeyTranslationKey()).getCode()
                  )
            );
         mc.options
            .sprintKey
            .setPressed(
               Sprint.INSTANCE.isOn() && !Sprint.INSTANCE.inWater()
                  || InputUtil.isKeyPressed(
                     mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.sprintKey.getBoundKeyTranslationKey()).getCode()
                  )
            );
         if (this.allowSneak.getValue()) {
            mc.options
               .sneakKey
               .setPressed(
                  InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode())
               );
         }
      }
   }

   @EventListener(priority = 100)
   public void keyboard(KeyboardInputEvent event) {
      if (this.sneak.getValue()) {
         event.cancel();
      }

      if (this.gui.getValue() && !(mc.currentScreen instanceof ChatScreen)) {
         for (KeyBinding k : new KeyBinding[]{mc.options.backKey, mc.options.leftKey, mc.options.rightKey}) {
            k.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(k.getBoundKeyTranslationKey()).getCode()));
         }

         mc.options
            .jumpKey
            .setPressed(
               ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.mode.is(ElytraFly.Mode.Bounce) && ElytraFly.INSTANCE.autoJump.getValue()
                  || InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.jumpKey.getBoundKeyTranslationKey()).getCode())
            );
         mc.options
            .forwardKey
            .setPressed(
               AutoWalk.INSTANCE.forward()
                  || InputUtil.isKeyPressed(
                     mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.forwardKey.getBoundKeyTranslationKey()).getCode()
                  )
            );
         mc.options
            .sprintKey
            .setPressed(
               Sprint.INSTANCE.isOn() && !Sprint.INSTANCE.inWater()
                  || InputUtil.isKeyPressed(
                     mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.sprintKey.getBoundKeyTranslationKey()).getCode()
                  )
            );
         if (this.allowSneak.getValue()) {
            mc.options
               .sneakKey
               .setPressed(
                  InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode())
               );
         }

         mc.player.input.pressingForward = mc.options.forwardKey.isPressed();
         mc.player.input.pressingBack = mc.options.backKey.isPressed();
         mc.player.input.pressingLeft = mc.options.leftKey.isPressed();
         mc.player.input.pressingRight = mc.options.rightKey.isPressed();
         mc.player.input.movementForward = getMovementMultiplier(mc.player.input.pressingForward, mc.player.input.pressingBack);
         mc.player.input.movementSideways = getMovementMultiplier(mc.player.input.pressingLeft, mc.player.input.pressingRight);
         mc.player.input.jumping = mc.options.jumpKey.isPressed();
         mc.player.input.sneaking = mc.options.sneakKey.isPressed();
      }
   }

   @EventListener
   public void onUse(InteractItemEvent event) {
      if (event.isPre()) {
         if (this.delay > 0) {
            mc.itemUseCooldown = 0;
            event.cancel();
         } else if (this.mode.is(NoSlow.Mode.GrimPacket)
            && mc.player != null
            && mc.player.getStackInHand(event.hand).getItem().getComponents().contains(DataComponentTypes.FOOD)) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 1, 0, SlotActionType.PICKUP, mc.player);
         }
      }
   }

   @EventListener
   public void onPacketSend(PacketEvent.Send e) {
      if (!nullCheck()) {
         if (this.mode.is(NoSlow.Mode.Drop)
            && e.getPacket() instanceof PlayerInteractItemC2SPacket packet
            && packet.getHand() == Hand.MAIN_HAND
            && mc.player.getMainHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD)) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.DROP_ITEM, BlockPos.ORIGIN, Direction.DOWN));
         } else if (MovementUtil.isMoving() && !this.pause.get()) {
            if (e.getPacket() instanceof ClickSlotC2SPacket click) {
               switch ((NoSlow.Bypass)this.clickBypass.getValue()) {
                  case NCP:
                     if (mc.player.isOnGround() && !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, 0.0656, 0.0)).iterator().hasNext()) {
                        if (mc.player.isSprinting()) {
                           mc.getNetworkHandler()
                              .sendPacket(
                                 new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.STOP_SPRINTING)
                              );
                        }

                        mc.getNetworkHandler()
                           .sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0656, mc.player.getZ(), false));
                     }
                     break;
                  case NCP2:
                     if (mc.player.isOnGround()
                        && !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, 2.71875E-7, 0.0)).iterator().hasNext()) {
                        if (mc.player.isSprinting()) {
                           mc.getNetworkHandler()
                              .sendPacket(
                                 new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.STOP_SPRINTING)
                              );
                        }

                        mc.getNetworkHandler()
                           .sendPacket(
                              new PositionAndOnGround(mc.player.getX(), mc.player.getY() + 2.71875E-7, mc.player.getZ(), false)
                           );
                     }
                     break;
                  case Grim:
                     if (click.getActionType() != SlotActionType.PICKUP && click.getActionType() != SlotActionType.PICKUP_ALL) {
                        mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
                     }
                     break;
                  case Delay:
                     this.storedClicks.add(click);
                     e.cancel();
               }
            }

            if (e.getPacket() instanceof CloseHandledScreenC2SPacket && this.clickBypass.is(NoSlow.Bypass.Delay)) {
               this.pause.set(true);

               while (!this.storedClicks.isEmpty()) {
                  mc.getNetworkHandler().sendPacket((Packet)this.storedClicks.poll());
               }

               this.pause.set(false);
            }
         }
      }
   }

   @EventListener
   public void onPacketSendPost(PacketEvent.Sent e) {
      if (e.getPacket() instanceof ClickSlotC2SPacket && mc.player.isSprinting() && this.clickBypass.is(NoSlow.Bypass.NCP)) {
         mc.getNetworkHandler()
            .sendPacket(new ClientCommandC2SPacket(mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_SPRINTING));
      }
   }

   public boolean noSlow() {
      return this.isOn()
         && this.mode.getValue() != NoSlow.Mode.None
         && (this.mode.getValue() != NoSlow.Mode.Drop && this.mode.getValue() != NoSlow.Mode.GrimPacket || this.using);
   }

   public boolean soulSand() {
      return this.isOn() && this.soulSand.getValue();
   }

   public boolean climb() {
      return this.isOn() && this.climb.getValue();
   }

   private static enum Bypass {
      None,
      NCP,
      NCP2,
      Grim,
      Delay;
   }

   public static enum Mode {
      Vanilla,
      NCP,
      Grim,
      GrimPacket,
      Drop,
      None;
   }
}
