package dev.luminous.asm.mixins;

import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.core.impl.ShaderManager;
import dev.luminous.core.impl.ShaderManager.Shader;
import dev.luminous.mod.modules.impl.player.Freecam;
import dev.luminous.mod.modules.impl.render.Chams;
import dev.luminous.mod.modules.impl.render.ShaderModule;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
   @Unique
   boolean renderingChams = false;
   @Unique
   boolean renderingEntity = false;

   @Inject(method = "renderEntity", at = @At("HEAD"))
   private void injectChamsForEntity(
      Entity entity,
      double cameraX,
      double cameraY,
      double cameraZ,
      float tickDelta,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      CallbackInfo ci
   ) {
      if (Chams.INSTANCE.isOn() && Chams.INSTANCE.throughWall.getValue()) {
         if (Chams.INSTANCE.chams(entity)) {
            if (this.renderingEntity) {
               Wrapper.mc.getBufferBuilders().getEntityVertexConsumers().draw();
               this.renderingEntity = false;
            }

            GL11.glEnable(32823);
            GL11.glPolygonOffset(1.0F, -1000000.0F);
            this.renderingChams = true;
         } else {
            this.renderingEntity = true;
         }
      }
   }

   @Inject(method = "renderEntity", at = @At("RETURN"))
   private void injectChamsForEntityPost(
      Entity entity,
      double cameraX,
      double cameraY,
      double cameraZ,
      float tickDelta,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      CallbackInfo ci
   ) {
      if (Chams.INSTANCE.isOn() && Chams.INSTANCE.throughWall.getValue() && this.renderingChams) {
         Wrapper.mc.getBufferBuilders().getEntityVertexConsumers().draw();
         GL11.glPolygonOffset(1.0F, 1000000.0F);
         GL11.glDisable(32823);
         this.renderingChams = false;
      }
   }

   @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/PostEffectProcessor;render(F)V", ordinal = 0), require = 0)
   void replaceShaderHook(PostEffectProcessor instance, float tickDelta) {
      ShaderManager.Shader shaders = (ShaderManager.Shader)ShaderModule.INSTANCE.mode.getValue();
      if (ShaderModule.INSTANCE.isOn() && Wrapper.mc.world != null) {
         Alien.SHADER.setupShader(shaders, Alien.SHADER.getShaderOutline(shaders));
      } else {
         instance.render(tickDelta);
      }
   }

   @ModifyArg(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"
      ),
      index = 3
   )
   private boolean renderSetupTerrainModifyArg(boolean spectator) {
      return Freecam.INSTANCE.isOn() || spectator;
   }
}
