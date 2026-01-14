package dev.luminous.mod.commands.impl;

import dev.luminous.mod.commands.Command;
import java.text.DecimalFormat;
import java.util.List;

public class ClipCommand extends Command {
   public ClipCommand() {
      super("clip", "[x] [y] [z]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length != 3) {
         this.sendUsage();
      } else if (this.isNumeric(parameters[0])) {
         double x = mc.player.getX() + Double.parseDouble(parameters[0]);
         if (this.isNumeric(parameters[1])) {
            double y = mc.player.getY() + Double.parseDouble(parameters[1]);
            if (this.isNumeric(parameters[2])) {
               double z = mc.player.getZ() + Double.parseDouble(parameters[2]);
               mc.player.setPosition(x, y, z);
               DecimalFormat df = new DecimalFormat("0.0");
               this.sendChatMessage("§fTeleported to §e" + df.format(x) + ", " + df.format(y) + ", " + df.format(z));
            } else {
               this.sendUsage();
            }
         } else {
            this.sendUsage();
         }
      } else {
         this.sendUsage();
      }
   }

   private boolean isNumeric(String str) {
      return str.matches("-?\\d+(\\.\\d+)?");
   }

   @Override
   public String[] getAutocorrect(int count, List<String> seperated) {
      return new String[]{"0 "};
   }
}
