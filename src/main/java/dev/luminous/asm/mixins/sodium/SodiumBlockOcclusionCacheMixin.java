package dev.luminous.asm.mixins.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.luminous.mod.modules.impl.render.Xray;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BlockOcclusionCache.class, remap = false)
public abstract class SodiumBlockOcclusionCacheMixin {
   @ModifyReturnValue(method = "shouldDrawSide", at = @At("RETURN"))
   private boolean shouldDrawSide(boolean original, BlockState state, BlockView view, BlockPos pos, Direction facing) {
      return Xray.INSTANCE.isOn() ? Xray.INSTANCE.modifyDrawSide(state, view, pos, facing, original) : original;
   }
}
