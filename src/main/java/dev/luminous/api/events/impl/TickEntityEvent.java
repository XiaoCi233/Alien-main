package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import net.minecraft.entity.Entity;

public class TickEntityEvent extends Event {
   private static final TickEntityEvent INSTANCE = new TickEntityEvent();
   private Entity entity;

   private TickEntityEvent() {
   }

   public static TickEntityEvent get(Entity entity) {
      INSTANCE.entity = entity;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
