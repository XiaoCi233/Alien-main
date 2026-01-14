package dev.luminous.asm.accessors;

import java.util.Map;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimpleRegistry.class)
public interface ISimpleRegistry<T> {
   @Accessor("valueToEntry")
   Map<T, Reference<T>> getValueToEntry();
}
