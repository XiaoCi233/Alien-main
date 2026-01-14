package dev.luminous.asm.mixins;

import dev.luminous.mod.modules.impl.render.Xray;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractBlockRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractBlockRenderContext.class)
public abstract class MixinAbstractBlockRenderContext {
   @Final
   @Shadow(remap = false)
   protected BlockRenderInfo blockInfo;

   @Inject(
      method = "renderQuad",
      at = @At(
         value = "INVOKE",
         target = "Lnet/fabricmc/fabric/impl/client/indigo/renderer/render/AbstractBlockRenderContext;bufferQuad(Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;Lnet/minecraft/client/render/VertexConsumer;)V"
      ),
      cancellable = true
   )
   private void onBufferQuad(MutableQuadViewImpl quad, CallbackInfo ci) {
      if (Xray.shouldBlock(this.blockInfo.blockState)) {
         ci.cancel();
      }
   }
}
