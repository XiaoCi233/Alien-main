package dev.luminous.mod.commands.impl;

import dev.luminous.Alien;
import dev.luminous.core.impl.PlayerManager;
import dev.luminous.mod.commands.Command;
import dev.luminous.mod.gui.windows.WindowsScreen;
import dev.luminous.mod.gui.windows.impl.ItemSelectWindow;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Items;

public class CleanerCommand extends Command {
   public CleanerCommand() {
      super("cleaner", "[\"\"/name/reset/clear/list] | [add/remove] [name]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         PlayerManager.screenToOpen = new WindowsScreen(new ItemSelectWindow(Alien.CLEANER));
      } else {
         String var2 = parameters[0];
         switch (var2) {
            case "reset":
               Alien.CLEANER.clear();
               Alien.CLEANER.add(Items.NETHERITE_SWORD.getTranslationKey());
               Alien.CLEANER.add(Items.NETHERITE_PICKAXE.getTranslationKey());
               Alien.CLEANER.add(Items.NETHERITE_HELMET.getTranslationKey());
               Alien.CLEANER.add(Items.NETHERITE_CHESTPLATE.getTranslationKey());
               Alien.CLEANER.add(Items.NETHERITE_LEGGINGS.getTranslationKey());
               Alien.CLEANER.add(Items.NETHERITE_BOOTS.getTranslationKey());
               Alien.CLEANER.add(Items.OBSIDIAN.getTranslationKey());
               Alien.CLEANER.add(Items.ENDER_CHEST.getTranslationKey());
               Alien.CLEANER.add(Items.ENDER_PEARL.getTranslationKey());
               Alien.CLEANER.add(Items.ENCHANTED_GOLDEN_APPLE.getTranslationKey());
               Alien.CLEANER.add(Items.EXPERIENCE_BOTTLE.getTranslationKey());
               Alien.CLEANER.add(Items.COBWEB.getTranslationKey());
               Alien.CLEANER.add(Items.POTION.getTranslationKey());
               Alien.CLEANER.add(Items.SPLASH_POTION.getTranslationKey());
               Alien.CLEANER.add(Items.TOTEM_OF_UNDYING.getTranslationKey());
               Alien.CLEANER.add(Items.END_CRYSTAL.getTranslationKey());
               Alien.CLEANER.add(Items.ELYTRA.getTranslationKey());
               Alien.CLEANER.add(Items.FLINT_AND_STEEL.getTranslationKey());
               Alien.CLEANER.add(Items.PISTON.getTranslationKey());
               Alien.CLEANER.add(Items.STICKY_PISTON.getTranslationKey());
               Alien.CLEANER.add(Items.REDSTONE_BLOCK.getTranslationKey());
               Alien.CLEANER.add(Items.GLOWSTONE.getTranslationKey());
               Alien.CLEANER.add(Items.RESPAWN_ANCHOR.getTranslationKey());
               Alien.CLEANER.add(Items.ANVIL.getTranslationKey());
               this.sendChatMessage("§fItems list got reset");
               return;
            case "clear":
               Alien.CLEANER.getList().clear();
               this.sendChatMessage("§fItems list got clear");
               return;
            case "list":
               if (Alien.CLEANER.getList().isEmpty()) {
                  this.sendChatMessage("§fItems list is empty");
                  return;
               }

               for (String name : Alien.CLEANER.getList()) {
                  this.sendChatMessage("§a" + name);
               }

               return;
            case "add":
               if (parameters.length == 2) {
                  Alien.CLEANER.add(parameters[1]);
                  this.sendChatMessage("§f" + parameters[1] + (Alien.CLEANER.inList(parameters[1]) ? " §ahas been added" : " §chas been removed"));
                  return;
               }

               this.sendUsage();
               return;
            case "remove":
               if (parameters.length == 2) {
                  Alien.CLEANER.remove(parameters[1]);
                  this.sendChatMessage("§f" + parameters[1] + (Alien.CLEANER.inList(parameters[1]) ? " §ahas been added" : " §chas been removed"));
                  return;
               }

               this.sendUsage();
               return;
            default:
               if (parameters.length == 1) {
                  this.sendChatMessage("§f" + parameters[0] + (Alien.CLEANER.inList(parameters[0]) ? " §ais in whitelist" : " §cisn't in whitelist"));
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
            if (input.equalsIgnoreCase(Alien.getPrefix() + "cleaner") || x.toLowerCase().startsWith(input)) {
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
