package dev.luminous.api.events.impl;

import net.minecraft.entity.player.PlayerEntity;

public class DeathEvent {
   private static final DeathEvent INSTANCE = new DeathEvent();
   private PlayerEntity player;

   private DeathEvent() {
   }

   public static DeathEvent get(PlayerEntity player) {
      INSTANCE.player = player;
      return INSTANCE;
   }

   public PlayerEntity getPlayer() {
      return this.player;
   }
}
