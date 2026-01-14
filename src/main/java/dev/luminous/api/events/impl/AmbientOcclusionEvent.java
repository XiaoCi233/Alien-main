package dev.luminous.api.events.impl;

public class AmbientOcclusionEvent {
   private static final AmbientOcclusionEvent INSTANCE = new AmbientOcclusionEvent();
   public float lightLevel = -1.0F;

   private AmbientOcclusionEvent() {
   }

   public static AmbientOcclusionEvent get() {
      INSTANCE.lightLevel = -1.0F;
      return INSTANCE;
   }
}
