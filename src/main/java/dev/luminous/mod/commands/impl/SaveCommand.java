package dev.luminous.mod.commands.impl;

import dev.luminous.Alien;
import dev.luminous.core.Manager;
import dev.luminous.core.impl.ConfigManager;
import dev.luminous.mod.commands.Command;
import java.io.File;
import java.util.List;

public class SaveCommand extends Command {
   public SaveCommand() {
      super("save", "");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 1) {
         this.sendChatMessage("§fSaving config named " + parameters[0]);
         File folder = new File(mc.runDirectory.getPath() + File.separator + "Alien".toLowerCase() + File.separator + "cfg");
         if (!folder.exists()) {
            folder.mkdirs();
         }

         ConfigManager.options = Manager.getFile("cfg" + File.separator + parameters[0] + ".cfg");
         Alien.save();
         ConfigManager.options = Manager.getFile("options.txt");
      } else {
         this.sendChatMessage("§fSaving..");
      }

      Alien.save();
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
