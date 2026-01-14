package dev.luminous.mod.modules.impl.client;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import java.io.PrintWriter;

public class IRC extends Module {
   public static PrintWriter printWriter;
   private static final String SERVER_HOST = "47.121.113.160";
   private static final int SERVER_PORT = 6667;
   public static volatile boolean connect;

   public IRC() {
      super("LowInputLatency", Module.Category.Client);
   }
}
