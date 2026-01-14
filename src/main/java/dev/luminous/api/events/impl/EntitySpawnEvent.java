package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import net.minecraft.entity.Entity;

public class EntitySpawnEvent extends Event {
   private static final EntitySpawnEvent INSTANCE = new EntitySpawnEvent();
   private Entity entity;

   private EntitySpawnEvent() {
   }

   public static EntitySpawnEvent get(Entity entity) {
      INSTANCE.entity = entity;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
