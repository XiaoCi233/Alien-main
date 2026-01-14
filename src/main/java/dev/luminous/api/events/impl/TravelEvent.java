package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import dev.luminous.api.events.Event;
import net.minecraft.entity.player.PlayerEntity;

public class TravelEvent extends Event {
   private static final TravelEvent INSTANCE = new TravelEvent();
   private PlayerEntity entity;

   private TravelEvent() {
   }

   public static TravelEvent get(Event.Stage stage, PlayerEntity entity) {
      INSTANCE.entity = entity;
      INSTANCE.stage = stage;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }

   public PlayerEntity getEntity() {
      return this.entity;
   }
}
