package dev.luminous.mod.modules.impl.render;

import com.google.common.collect.Maps;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.render.ModelPlayer;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.asm.accessors.IEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.client.TextRadar;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class LogoutSpots extends Module {
   private final ColorSetting box = this.add(new ColorSetting("Box", new Color(255, 255, 255, 100)).injectBoolean(true));
   private final ColorSetting fill = this.add(new ColorSetting("Fill", new Color(255, 255, 255, 100)).injectBoolean(true));
   private final ColorSetting text = this.add(new ColorSetting("Text", new Color(255, 255, 255, 255)).injectBoolean(true));
   private final ColorSetting chamsFill = this.add(new ColorSetting("ChamsFill", new Color(255, 255, 255, 100)).injectBoolean(true));
   private final ColorSetting chamsLine = this.add(new ColorSetting("ChamsLine", new Color(255, 255, 255, 100)).injectBoolean(true));
   final Map<UUID, PlayerEntity> playerCache = Maps.newConcurrentMap();
   final Map<UUID, ModelPlayer> logoutCache = Maps.newConcurrentMap();
   private final BooleanSetting health = this.add(new BooleanSetting("Health", true));
   private final BooleanSetting totem = this.add(new BooleanSetting("Totem", true));
   private final BooleanSetting message = this.add(new BooleanSetting("Message", true));

   public LogoutSpots() {
      super("LogoutSpots", Module.Category.Render);
      this.setChinese("退出记录");
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!nullCheck()) {
         if (event.getPacket() instanceof PlayerListS2CPacket packet) {
            if (packet.getActions().contains(Action.ADD_PLAYER)) {
               for (Entry addedPlayer : packet.getPlayerAdditionEntries()) {
                  if (addedPlayer.gameMode() != GameMode.SPECTATOR) {
                     for (UUID uuid : this.logoutCache.keySet()) {
                        if (uuid.equals(addedPlayer.profile().getId())) {
                           PlayerEntity player = ((ModelPlayer)this.logoutCache.get(uuid)).player;
                           if (this.message.getValue()) {
                              mc.execute(
                                 () -> this.sendMessage(
                                    "§f"
                                       + player.getName().getString()
                                       + " §rLogged back at §f"
                                       + player.getBlockX()
                                       + ", "
                                       + player.getBlockY()
                                       + ", "
                                       + player.getBlockZ()
                                 )
                              );
                           }

                           this.logoutCache.remove(uuid);
                        }
                     }
                  }
               }
            }
         } else if (event.getPacket() instanceof PlayerRemoveS2CPacket(java.util.List<UUID> profileIds)) {
            for (UUID uuid2 : profileIds) {
               for (UUID uuidx : this.playerCache.keySet()) {
                  if (uuidx.equals(uuid2)) {
                     PlayerEntity player = (PlayerEntity)this.playerCache.get(uuidx);
                     if (!this.logoutCache.containsKey(uuidx) && player != null) {
                        ModelPlayer modelPlayer = new ModelPlayer(player);
                        if (this.message.getValue()) {
                           mc.execute(
                              () -> this.sendMessage(
                                 "§f"
                                    + player.getName().getString()
                                    + " §rLogged out at §f"
                                    + player.getBlockX()
                                    + ", "
                                    + player.getBlockY()
                                    + ", "
                                    + player.getBlockZ()
                              )
                           );
                        }

                        this.logoutCache.put(uuidx, modelPlayer);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onDisable() {
      this.playerCache.clear();
      this.logoutCache.clear();
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.playerCache.clear();

      for (AbstractClientPlayerEntity player : Alien.THREAD.getPlayers()) {
         if (player != null && !player.equals(mc.player)) {
            this.playerCache.put(player.getGameProfile().getId(), player);
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack matrixStack) {
      for (ModelPlayer data : this.logoutCache.values()) {
         PlayerEntity player = data.player;
         Box box = ((IEntity)player).getDimensions().getBoxAt(player.getPos());
         if (this.box.booleanValue) {
            Render3DUtil.drawBox(matrixStack, box, this.box.getValue());
         }

         if (this.fill.booleanValue) {
            Render3DUtil.drawFill(matrixStack, box, this.fill.getValue());
         }

         if (this.chamsFill.booleanValue || this.chamsLine.booleanValue) {
            data.render(matrixStack, this.chamsFill, this.chamsLine);
         }

         if (this.text.booleanValue) {
            Render3DUtil.drawText3D(
               player.getName().getString()
                  + (this.health.getValue() ? TextRadar.getHealthColor(player) + " " + round2(player.getHealth() + player.getAbsorptionAmount()) : "")
                  + (
                     this.totem.getValue() && Alien.POP.getPop(player) > 0
                        ? TextRadar.getPopColor(Alien.POP.getPop(player)) + " -" + Alien.POP.getPop(player)
                        : ""
                  ),
               new Vec3d(player.getX(), ((IEntity)player).getDimensions().getBoxAt(player.getPos()).maxY + 0.5, player.getZ()),
               this.text.getValue()
            );
         }
      }
   }

   public static float round2(double value) {
      BigDecimal bd = new BigDecimal(value);
      bd = bd.setScale(1, RoundingMode.HALF_UP);
      return bd.floatValue();
   }
}
