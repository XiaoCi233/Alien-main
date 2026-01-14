package dev.luminous.mod.commands.impl;

import dev.luminous.Alien;
import dev.luminous.core.impl.PlayerManager;
import dev.luminous.mod.commands.Command;
import dev.luminous.mod.gui.windows.WindowsScreen;
import dev.luminous.mod.gui.windows.impl.ItemSelectWindow;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Items;

public class TradeCommand extends Command {
   public TradeCommand() {
      super("trade", "[\"\"/name/reset/clear/list] | [add/remove] [name]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         PlayerManager.screenToOpen = new WindowsScreen(new ItemSelectWindow(Alien.TRADE));
      } else {
         String var2 = parameters[0];
         switch (var2) {
            case "reset":
               Alien.TRADE.clear();
               Alien.TRADE.add(Items.ENCHANTED_BOOK.getTranslationKey());
               Alien.TRADE.add(Items.DIAMOND_BLOCK.getTranslationKey());
               this.sendChatMessage("§fItems list got reset");
               return;
            case "clear":
               Alien.TRADE.clear();
               this.sendChatMessage("§fItems list got clear");
               return;
            case "list":
               if (Alien.TRADE.getList().isEmpty()) {
                  this.sendChatMessage("§fItems list is empty");
                  return;
               }

               for (String name : Alien.TRADE.getList()) {
                  this.sendChatMessage("§a" + name);
               }

               return;
            case "add":
               if (parameters.length == 2) {
                  Alien.TRADE.add(parameters[1]);
                  this.sendChatMessage("§f" + parameters[1] + (Alien.TRADE.inWhitelist(parameters[1]) ? " §ahas been added" : " §chas been removed"));
                  return;
               }

               this.sendUsage();
               return;
            case "remove":
               if (parameters.length == 2) {
                  Alien.TRADE.remove(parameters[1]);
                  this.sendChatMessage("§f" + parameters[1] + (Alien.TRADE.inWhitelist(parameters[1]) ? " §ahas been added" : " §chas been removed"));
                  return;
               }

               this.sendUsage();
               return;
            default:
               if (parameters.length == 1) {
                  this.sendChatMessage("§f" + parameters[0] + (Alien.TRADE.inWhitelist(parameters[0]) ? " §ais in whitelist" : " §cisn't in whitelist"));
               } else {
                  this.sendUsage();
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

         for (String x : List.of("add", "remove", "list", "reset", "clear")) {
            if (input.equalsIgnoreCase(Alien.getPrefix() + "trade") || x.toLowerCase().startsWith(input)) {
               correct.add(x);
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
