package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import net.minecraft.client.gui.screen.Screen;

public class OpenScreenEvent extends Event {
   private static final OpenScreenEvent INSTANCE = new OpenScreenEvent();
   public Screen screen;

   private OpenScreenEvent() {
   }

   public static OpenScreenEvent get(Screen screen) {
      INSTANCE.screen = screen;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }
}
