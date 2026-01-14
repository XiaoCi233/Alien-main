package dev.luminous.mod.modules.impl.render;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

public class Fov extends Module {
   public static Fov INSTANCE;
   public final SliderSetting fov = this.add(new SliderSetting("Fov", 90.0, 30.0, 170.0, 1.0));
   public final SliderSetting itemFov = this.add(new SliderSetting("ItemFov", 70.0, 30.0, 170.0, 1.0));

   public Fov() {
      super("Fov", Module.Category.Render);
      this.setChinese("自定义视角");
      INSTANCE = this;
   }
}
