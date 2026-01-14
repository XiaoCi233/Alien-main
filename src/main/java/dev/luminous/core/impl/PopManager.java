package dev.luminous.core.impl;


import dev.luminous.Alien;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.DeathEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClickGui;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

public class PopManager implements Wrapper {
   public final HashMap<String, Integer> popContainer = new HashMap();
   private final List<PlayerEntity> deadPlayer = new ArrayList();

   public PopManager() {
      this.init();
   }

   
   public void init() {
      Alien.EVENT_BUS.subscribe(this);
   }

   public int getPop(String s) {
      return (Integer)this.popContainer.getOrDefault(s, 0);
   }

   public int getPop(PlayerEntity player) {
      return this.getPop(player.getName().getString());
   }

   public void onUpdate() {
      if (!Module.nullCheck()) {
         for (AbstractClientPlayerEntity player : Alien.THREAD.getPlayers()) {
            if (player == null || !player.isDead()) {
               this.deadPlayer.remove(player);
            } else if (!this.deadPlayer.contains(player)) {
               Alien.EVENT_BUS.post(DeathEvent.get(player));
               this.onDeath(player);
               this.deadPlayer.add(player);
            }
         }
      }
   }

   @EventListener
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!Module.nullCheck()) {
         if (event.getPacket() instanceof EntityStatusS2CPacket packet && packet.getStatus() == 35 && packet.getEntity(mc.world) instanceof PlayerEntity player
            )
          {
            this.onTotemPop(player);
         }
      }
   }

   public void onDeath(PlayerEntity player) {
      this.popContainer.remove(player.getName().getString());
   }

   public void onTotemPop(PlayerEntity player) {
      int l_Count = 1;
      if (this.popContainer.containsKey(player.getName().getString())) {
         l_Count = (Integer)this.popContainer.get(player.getName().getString());
         this.popContainer.put(player.getName().getString(), ++l_Count);
      } else {
         this.popContainer.put(player.getName().getString(), l_Count);
      }

      Alien.EVENT_BUS.post(TotemEvent.get(player));
   }
}
