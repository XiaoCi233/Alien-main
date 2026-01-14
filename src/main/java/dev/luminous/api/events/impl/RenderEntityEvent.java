package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import net.minecraft.entity.Entity;

public class RenderEntityEvent extends Event {
   private static final RenderEntityEvent INSTANCE = new RenderEntityEvent();
   private Entity entity;

   private RenderEntityEvent() {
   }

   public static RenderEntityEvent get(Entity entity) {
      INSTANCE.entity = entity;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
