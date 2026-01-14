package dev.luminous.mod.modules.impl.player;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;

public class AntiEffects extends Module {
   public static AntiEffects INSTANCE;
   public final BooleanSetting levitation = this.add(new BooleanSetting("Levitation", true));
   public final BooleanSetting slowFalling = this.add(new BooleanSetting("SlowFalling", true));

   public AntiEffects() {
      super("AntiEffects", Module.Category.Player);
      this.setChinese("反效果");
      INSTANCE = this;
   }
}
