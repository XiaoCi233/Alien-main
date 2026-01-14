package dev.luminous.mod.commands.impl;

import dev.luminous.mod.commands.Command;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import java.util.List;

public class PrefixCommand extends Command {
   public PrefixCommand() {
      super("prefix", "[prefix]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else if (parameters[0].startsWith("/")) {
         this.sendChatMessage("§fPlease specify a valid §bprefix.");
      } else {
         ClientSetting.INSTANCE.prefix.setValue(parameters[0]);
         this.sendChatMessage("§bPrefix §fset to §e" + parameters[0]);
      }
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return null;
   }
}
