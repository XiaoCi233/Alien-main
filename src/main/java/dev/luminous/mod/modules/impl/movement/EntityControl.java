package dev.luminous.mod.modules.impl.movement;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;

public class EntityControl extends Module {
   public static EntityControl INSTANCE;

   public EntityControl() {
      super("EntityControl", Module.Category.Movement);
      this.setChinese("骑行控制");
      INSTANCE = this;
   }
}
