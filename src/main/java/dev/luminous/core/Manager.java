package dev.luminous.core;

import java.io.File;
import net.minecraft.client.MinecraftClient;

public class Manager {
   public static final MinecraftClient mc = MinecraftClient.getInstance();

   public static File getFile(String s) {
      File folder = getFolder();
      return new File(folder, s);
   }

   public static File getFolder() {
      File folder = new File(mc.runDirectory.getPath() + File.separator + "Alien".toLowerCase());
      if (!folder.exists()) {
         folder.mkdirs();
      }

      return folder;
   }
}
