package dev.luminous.api.events.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;

public class RemoveEntityEvent {
   public static final RemoveEntityEvent instance = new RemoveEntityEvent();
   private Entity entity;
   private RemovalReason removalReason;

   private RemoveEntityEvent() {
   }

   public static RemoveEntityEvent get(Entity entity, RemovalReason removalReason) {
      instance.entity = entity;
      instance.removalReason = removalReason;
      return instance;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public RemovalReason getRemovalReason() {
      return this.removalReason;
   }
}
