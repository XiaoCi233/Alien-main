package dev.luminous.mod.commands.impl;

import dev.luminous.Alien;
import dev.luminous.mod.commands.Command;
import dev.luminous.mod.modules.impl.misc.FakePlayer;
import java.util.ArrayList;
import java.util.List;

public class FakePlayerCommand extends Command {
   public FakePlayerCommand() {
      super("fakeplayer", "[record/play]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         FakePlayer.INSTANCE.toggle();
      } else {
         switch (parameters[0]) {
            case "record":
               FakePlayer.INSTANCE.record.setValue(!FakePlayer.INSTANCE.record.getValue());
               break;
            case "play":
               FakePlayer.INSTANCE.play.setValue(!FakePlayer.INSTANCE.play.getValue());
               break;
            case null:
            default:
               this.sendUsage();
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

         for (String x : List.of("record", "play")) {
            if (input.equalsIgnoreCase(Alien.getPrefix() + "fakeplayer") || x.toLowerCase().startsWith(input)) {
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
