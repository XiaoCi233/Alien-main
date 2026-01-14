package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import net.minecraft.entity.Entity;

public class LookDirectionEvent extends Event {
   private static final LookDirectionEvent instance = new LookDirectionEvent();
   private Entity entity;
   private double cursorDeltaX;
   private double cursorDeltaY;

   private LookDirectionEvent() {
   }

   public static LookDirectionEvent get(Entity entity, double cursorDeltaX, double cursorDeltaY) {
      instance.entity = entity;
      instance.cursorDeltaX = cursorDeltaX;
      instance.cursorDeltaY = cursorDeltaY;
      instance.setCancelled(false);
      return instance;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public double getCursorDeltaX() {
      return this.cursorDeltaX;
   }

   public double getCursorDeltaY() {
      return this.cursorDeltaY;
   }
}
