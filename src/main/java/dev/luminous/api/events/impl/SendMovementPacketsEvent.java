package dev.luminous.api.events.impl;

public class SendMovementPacketsEvent {
   public static final SendMovementPacketsEvent instance = new SendMovementPacketsEvent();
   private float yaw;
   private float pitch;

   private SendMovementPacketsEvent() {
   }

   public static SendMovementPacketsEvent get(float yaw, float pitch) {
      instance.yaw = yaw;
      instance.pitch = pitch;
      return instance;
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }
}
