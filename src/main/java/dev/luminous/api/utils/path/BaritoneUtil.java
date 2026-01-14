package dev.luminous.api.utils.path;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.modules.impl.client.BaritoneModule;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public class BaritoneUtil implements Wrapper {
   public static boolean loaded;

   public static void gotoPos(BlockPos pos) {
      if (loaded) {
         BaritoneModule.gotoPos(pos);
      }
   }

   public static void forward() {
      if (loaded) {
         BaritoneModule.forward();
      }
   }

   public static void mine(Block block) {
      if (loaded) {
         BaritoneModule.mine(block);
      }
   }

   public static boolean isPathing() {
      return loaded ? BaritoneModule.isPathing() : false;
   }

   public static void cancelEverything() {
      if (loaded) {
         BaritoneModule.cancelEverything();
      }
   }

   public static boolean isActive() {
      return loaded ? BaritoneModule.isActive() : false;
   }

   static {
      Package[] packages = Package.getPackages();

      for (Package pkg : packages) {
         if (pkg.getName().contains("baritone.api")) {
            loaded = true;
            break;
         }
      }
   }
}
