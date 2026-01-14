package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class FireworkShooterRotationEvent extends Event {
   private static final FireworkShooterRotationEvent instance = new FireworkShooterRotationEvent();
   public LivingEntity shooter;
   public float pitch;
   public float yaw;

   private FireworkShooterRotationEvent() {
   }

   public static FireworkShooterRotationEvent get(LivingEntity shooter, float yaw, float pitch) {
      instance.shooter = shooter;
      instance.yaw = yaw;
      instance.pitch = pitch;
      instance.setCancelled(false);
      return instance;
   }

   public final Vec3d getRotationVector() {
      float f = this.pitch * (float) (Math.PI / 180.0);
      float g = -this.yaw * (float) (Math.PI / 180.0);
      float h = MathHelper.cos(g);
      float i = MathHelper.sin(g);
      float j = MathHelper.cos(f);
      float k = MathHelper.sin(f);
      return new Vec3d(i * j, -k, h * j);
   }
}
