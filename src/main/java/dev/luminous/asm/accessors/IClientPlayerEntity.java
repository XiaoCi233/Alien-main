package dev.luminous.asm.accessors;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerEntity.class)
public interface IClientPlayerEntity {
   @Accessor("ticksSinceLastPositionPacketSent")
   void setTicksSinceLastPositionPacketSent(int var1);

   @Accessor("lastYaw")
   float getLastYaw();

   @Accessor("lastYaw")
   void setLastYaw(float var1);

   @Accessor("lastPitch")
   float getLastPitch();

   @Accessor("lastPitch")
   void setLastPitch(float var1);
}
