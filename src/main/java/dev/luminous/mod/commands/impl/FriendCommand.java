package dev.luminous.mod.commands.impl;

import dev.luminous.Alien;
import dev.luminous.mod.commands.Command;
import java.util.ArrayList;
import java.util.List;

public class FriendCommand extends Command {
   public FriendCommand() {
      super("friend", "[name/reset/list] | [add/remove] [name]");
   }

   @Override
   public void runCommand(String[] parameters) {
      if (parameters.length == 0) {
         this.sendUsage();
      } else {
         String var2 = parameters[0];
         switch (var2) {
            case "reset":
               Alien.FRIEND.friendList.clear();
               this.sendChatMessage("§fFriends list got reset");
               return;
            case "list":
               if (Alien.FRIEND.friendList.isEmpty()) {
                  this.sendChatMessage("§fFriends list is empty");
                  return;
               } else {
                  StringBuilder friends = new StringBuilder();
                  int time = 0;
                  boolean first = true;
                  boolean start = true;

                  for (String name : Alien.FRIEND.friendList) {
                     if (!first) {
                        friends.append(", ");
                     }

                     friends.append(name);
                     first = false;
                     if (++time > 3) {
                        this.sendChatMessage((start ? "§eFriends §a" : "§a") + friends);
                        friends = new StringBuilder();
                        start = false;
                        first = true;
                        time = 0;
                     }
                  }

                  if (first) {
                     this.sendChatMessage("§a" + friends);
                  }

                  return;
               }
            case "add":
               if (parameters.length == 2) {
                  Alien.FRIEND.add(parameters[1]);
                  this.sendChatMessage("§f" + parameters[1] + (Alien.FRIEND.isFriend(parameters[1]) ? " §ahas been friended" : " §chas been unfriended"));
                  return;
               }

               this.sendUsage();
               return;
            case "remove":
            case "del":
               if (parameters.length == 2) {
                  Alien.FRIEND.remove(parameters[1]);
                  this.sendChatMessage("§f" + parameters[1] + (Alien.FRIEND.isFriend(parameters[1]) ? " §ahas been friended" : " §chas been unfriended"));
                  return;
               }

               this.sendUsage();
               return;
            default:
               if (parameters.length == 1) {
                  this.sendChatMessage("§f" + parameters[0] + (Alien.FRIEND.isFriend(parameters[0]) ? " §ais friended" : " §cisn't friended"));
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

         for (String x : List.of("add", "remove", "list", "reset")) {
            if (input.equalsIgnoreCase(Alien.getPrefix() + "friend") || x.toLowerCase().startsWith(input)) {
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
