package dev.luminous.mod.modules.impl.movement;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;

public class MovementSync extends Module {
   public static MovementSync INSTANCE;

   public MovementSync() {
      super("MovementSync", Module.Category.Movement);
      this.setChinese("移动同步");
      INSTANCE = this;
   }
}
