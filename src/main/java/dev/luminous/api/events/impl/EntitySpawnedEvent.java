package dev.luminous.api.events.impl;

import net.minecraft.entity.Entity;

public class EntitySpawnedEvent {
   private static final EntitySpawnedEvent INSTANCE = new EntitySpawnedEvent();
   private Entity entity;

   private EntitySpawnedEvent() {
   }

   public static EntitySpawnedEvent get(Entity player) {
      INSTANCE.entity = player;
      return INSTANCE;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
