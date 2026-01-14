package dev.luminous.mod.commands.impl;

import dev.luminous.Alien;
import dev.luminous.mod.commands.Command;
import dev.luminous.mod.modules.Module;
import java.util.ArrayList;
import java.util.List;

public class BindCommand extends Command {
   public BindCommand() {
      super("bind", "[module] [key]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else {
         String moduleName = parameters[0];
         Module module = Alien.MODULE.getModuleByName(moduleName);
         if (module == null) {
            this.sendChatMessage("§4Unknown module!");
         } else if (parameters.length == 1) {
            this.sendChatMessage("§fPlease specify a §bkey§f.");
         } else {
            String rkey = parameters[1];
            if (rkey == null) {
               this.sendChatMessage("§4Unknown Error");
            } else {
               if (module.setBind(rkey.toUpperCase())) {
                  this.sendChatMessage("§fBind for §r" + module.getName() + "§f set to §r" + rkey.toUpperCase());
               }
            }
         }
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      if (count != 1) {
         return null;
      } else {
         String input = ((String)seperated.getLast()).toLowerCase();
         List<String> correct = new ArrayList();

         for (Module x : Alien.MODULE.getModules()) {
            if (input.equalsIgnoreCase(Alien.getPrefix() + "bind") || x.getName().toLowerCase().startsWith(input)) {
               correct.add(x.getName());
            }
         }

         int numCmds = correct.size();
         String[] commands = new String[numCmds];
         int i = 0;

         for (String xx : correct) {
            commands[i++] = xx;
         }

         return commands;
      }
   }
}
