package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.ProgressScreen;

public class NoTerrainScreen extends Module {
   public NoTerrainScreen() {
      super("NoTerrainScreen", Module.Category.Misc);
      this.setChinese("没有加载界面");
   }

   @EventListener
   public void onEvent(ClientTickEvent event) {
      if (!nullCheck()) {
         if (mc.currentScreen instanceof DownloadingTerrainScreen || mc.currentScreen instanceof ProgressScreen) {
            mc.currentScreen = null;
         }
      }
   }
}
