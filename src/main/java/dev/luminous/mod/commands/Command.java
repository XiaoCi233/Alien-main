package dev.luminous.mod.commands;

import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.core.impl.CommandManager;
import java.util.List;
import java.util.Objects;

public abstract class Command implements Wrapper {
   protected final String name;
   protected final String syntax;

   public Command(String name, String syntax) {
      this.name = (String)Objects.requireNonNull(name);
      this.syntax = (String)Objects.requireNonNull(syntax);
   }

   public String getName() {
      return this.name;
   }

   public String getSyntax() {
      return this.syntax;
   }

   public abstract void runCommand(String[] var1);

   public abstract String[] getAutocorrect(int var1, List<String> var2);

   public void sendUsage() {
      this.sendChatMessage("§4Parameter error §r" + Alien.getPrefix() + this.getName() + " §7" + this.getSyntax());
   }

   public void sendChatMessage(String message) {
      CommandManager.sendMessage(message);
   }
}
