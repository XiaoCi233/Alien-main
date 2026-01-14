package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.EntityVelocityUpdateEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.combat.Criticals;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.hit.BlockHitResult;

public class PacketLogger extends Module {
   public static PacketLogger INSTANCE;
   private final BooleanSetting moveFull = this.add(new BooleanSetting("MoveFull", true));
   private final BooleanSetting movePos = this.add(new BooleanSetting("MovePosition", true));
   private final BooleanSetting moveLook = this.add(new BooleanSetting("MoveLook", true));
   private final BooleanSetting moveGround = this.add(new BooleanSetting("MoveGround", true));
   private final BooleanSetting vehicleMove = this.add(new BooleanSetting("VehicleMove", true));
   private final BooleanSetting playerAction = this.add(new BooleanSetting("PlayerAction", true));
   private final BooleanSetting updateSlot = this.add(new BooleanSetting("UpdateSlot", true));
   private final BooleanSetting handSwing = this.add(new BooleanSetting("HandSwing", true));
   private final BooleanSetting pong = this.add(new BooleanSetting("Pong", true));
   private final BooleanSetting interactEntity = this.add(new BooleanSetting("InteractEntity", true));
   private final BooleanSetting interactBlock = this.add(new BooleanSetting("InteractBlock", true));
   private final BooleanSetting interactItem = this.add(new BooleanSetting("InteractItem", true));
   private final BooleanSetting closeScreen = this.add(new BooleanSetting("CloseScreen", true));
   private final BooleanSetting command = this.add(new BooleanSetting("ClientCommand", true));
   private final BooleanSetting status = this.add(new BooleanSetting("ClientStatus", true));
   private final BooleanSetting clickSlot = this.add(new BooleanSetting("ClickSlot", true));
   private final BooleanSetting pickInventory = this.add(new BooleanSetting("PickInventory", true));
   private final BooleanSetting teleportConfirm = this.add(new BooleanSetting("TeleportConfirm", true));
   private final BooleanSetting s2cVelocity = this.add(new BooleanSetting("S2cVelocity", true));

   public PacketLogger() {
      super("PacketLogger", Module.Category.Misc);
      this.setChinese("数据包记录");
      INSTANCE = this;
   }

   private void logPacket(String msg, Object... args) {
      String s = String.format(msg, args);
      CommandManager.sendMessage(s);
   }

   @EventListener(priority = 999999)
   public void velocity(EntityVelocityUpdateEvent event) {
      if (this.s2cVelocity.getValue() && event.getEntity() == mc.player) {
         this.logPacket("S2C Velocity, x: %s, y: %s, z: %s, isExplosion: %s", event.getX(), event.getY(), event.getZ(), event.isExplosion());
      }
   }

   @EventListener(priority = -999999)
   public void onPacketSend(PacketEvent.Send event) {
      if (event.getPacket() instanceof Full packet && this.moveFull.getValue()) {
         StringBuilder builder = new StringBuilder();
         builder.append("PlayerMove Full - ");
         if (packet.changesPosition()) {
            builder.append("x: ")
               .append(packet.getX(0.0))
               .append(", y: ")
               .append(packet.getY(0.0))
               .append(", z: ")
               .append(packet.getZ(0.0))
               .append(" ");
         }

         if (packet.changesLook()) {
            builder.append("yaw: ").append(packet.getYaw(0.0F)).append(", pitch: ").append(packet.getPitch(0.0F)).append(" ");
         }

         builder.append(" onground: ").append(packet.isOnGround());
         this.logPacket(builder.toString());
      }

      if (event.getPacket() instanceof PositionAndOnGround packet && this.movePos.getValue()) {
         StringBuilder builderx = new StringBuilder();
         builderx.append("PlayerMove PosGround - ");
         if (packet.changesPosition()) {
            builderx.append("x: ")
               .append(packet.getX(0.0))
               .append(", y: ")
               .append(packet.getY(0.0))
               .append(", z: ")
               .append(packet.getZ(0.0))
               .append(" ");
         }

         builderx.append(" onground: ").append(packet.isOnGround());
         this.logPacket(builderx.toString());
      }

      if (event.getPacket() instanceof LookAndOnGround packet && this.moveLook.getValue()) {
         StringBuilder builderx = new StringBuilder();
         builderx.append("PlayerMove LookGround - ");
         if (packet.changesLook()) {
            builderx.append("yaw: ").append(packet.getYaw(0.0F)).append(", pitch: ").append(packet.getPitch(0.0F)).append(" ");
         }

         builderx.append(" onground: ").append(packet.isOnGround());
         this.logPacket(builderx.toString());
      }

      if (event.getPacket() instanceof OnGroundOnly packet && this.moveGround.getValue()) {
         String s = "PlayerMove Ground - onground: " + packet.isOnGround();
         this.logPacket(s);
      }

      if (event.getPacket() instanceof VehicleMoveC2SPacket packet && this.vehicleMove.getValue()) {
         this.logPacket(
            "VehicleMove - x: %s, y: %s, z: %s, yaw: %s, pitch: %s",
            packet.getX(),
            packet.getY(),
            packet.getZ(),
            packet.getYaw(),
            packet.getPitch()
         );
      }

      if (event.getPacket() instanceof PlayerActionC2SPacket packet && this.playerAction.getValue() && packet.getDirection() != null) {
         this.logPacket(
            "PlayerAction - action: %s, direction: %s, pos: %s", packet.getAction().name(), packet.getDirection().name(), packet.getPos().toShortString()
         );
      }

      if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket packet && this.updateSlot.getValue()) {
         this.logPacket("UpdateSlot - slot: %d", packet.getSelectedSlot());
      }

      if (event.getPacket() instanceof HandSwingC2SPacket packet && this.handSwing.getValue()) {
         this.logPacket("HandSwing - hand: %s", packet.getHand().name());
      }

      if (event.getPacket() instanceof CommonPongC2SPacket packet && this.pong.getValue()) {
         this.logPacket("Pong - %d", packet.getParameter());
      }

      if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet && this.interactEntity.getValue()) {
         this.logPacket("InteractEntity - Entity: %s, id: %s", Criticals.getEntity(packet).getName().getString(), Criticals.getEntity(packet).getId());
      }

      if (event.getPacket() instanceof PlayerInteractBlockC2SPacket packet && this.interactBlock.getValue()) {
         BlockHitResult blockHitResult = packet.getBlockHitResult();
         this.logPacket(
            "InteractBlock - pos: %s, dir: %s, hand: %s", blockHitResult.getBlockPos().toShortString(), blockHitResult.getSide().name(), packet.getHand().name()
         );
      }

      if (event.getPacket() instanceof PlayerInteractItemC2SPacket packet && this.interactItem.getValue()) {
         this.logPacket("InteractItem - hand: %s", packet.getHand().name());
      }

      if (event.getPacket() instanceof CloseHandledScreenC2SPacket packet && this.closeScreen.getValue()) {
         this.logPacket("CloseScreen - id: %s", packet.getSyncId());
      }

      if (event.getPacket() instanceof ClientCommandC2SPacket packet && this.command.getValue()) {
         this.logPacket("ClientCommand - mode: %s", packet.getMode().name());
      }

      if (event.getPacket() instanceof ClientStatusC2SPacket packet && this.status.getValue()) {
         this.logPacket("ClientStatus - mode: %s", packet.getMode().name());
      }

      if (event.getPacket() instanceof ClickSlotC2SPacket packet && this.clickSlot.getValue()) {
         this.logPacket(
            "ClickSlot - type: %s, slot: %s, button: %s, id: %s", packet.getActionType().name(), packet.getSlot(), packet.getButton(), packet.getSyncId()
         );
      }

      if (event.getPacket() instanceof PickFromInventoryC2SPacket packet && this.pickInventory.getValue()) {
         this.logPacket("PickInventory - slot: %s", packet.getSlot());
      }

      if (event.getPacket() instanceof TeleportConfirmC2SPacket packet && this.teleportConfirm.getValue()) {
         this.logPacket("TeleportConfirm - id: %s", packet.getTeleportId());
      }
   }
}
