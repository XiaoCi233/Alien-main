package dev.luminous.core.impl;

import dev.luminous.core.Manager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import net.minecraft.item.Items;
import org.apache.commons.io.IOUtils;

public class CleanerManager extends Manager {
   private final ArrayList<String> list = new ArrayList();

   public CleanerManager() {
      this.read();
   }

   public ArrayList<String> getList() {
      return this.list;
   }

   public boolean inList(String name) {
      return this.list.contains(name) || this.list.contains(name.replace("block.minecraft.", "").replace("item.minecraft.", ""));
   }

   public void clear() {
      this.list.clear();
   }

   public void remove(String name) {
      name = name.replace("block.minecraft.", "").replace("item.minecraft.", "");
      this.list.remove(name);
   }

   public void add(String name) {
      name = name.replace("block.minecraft.", "").replace("item.minecraft.", "");
      if (!this.list.contains(name)) {
         this.list.add(name);
      }
   }

   public void read() {
      try {
         File friendFile = getFile("cleaner.txt");
         if (!friendFile.exists()) {
            this.add(Items.NETHERITE_SWORD.getTranslationKey());
            this.add(Items.NETHERITE_PICKAXE.getTranslationKey());
            this.add(Items.NETHERITE_HELMET.getTranslationKey());
            this.add(Items.NETHERITE_CHESTPLATE.getTranslationKey());
            this.add(Items.NETHERITE_LEGGINGS.getTranslationKey());
            this.add(Items.NETHERITE_BOOTS.getTranslationKey());
            this.add(Items.OBSIDIAN.getTranslationKey());
            this.add(Items.ENDER_CHEST.getTranslationKey());
            this.add(Items.ENDER_PEARL.getTranslationKey());
            this.add(Items.ENCHANTED_GOLDEN_APPLE.getTranslationKey());
            this.add(Items.EXPERIENCE_BOTTLE.getTranslationKey());
            this.add(Items.COBWEB.getTranslationKey());
            this.add(Items.POTION.getTranslationKey());
            this.add(Items.SPLASH_POTION.getTranslationKey());
            this.add(Items.TOTEM_OF_UNDYING.getTranslationKey());
            this.add(Items.END_CRYSTAL.getTranslationKey());
            this.add(Items.ELYTRA.getTranslationKey());
            this.add(Items.FLINT_AND_STEEL.getTranslationKey());
            this.add(Items.PISTON.getTranslationKey());
            this.add(Items.STICKY_PISTON.getTranslationKey());
            this.add(Items.REDSTONE_BLOCK.getTranslationKey());
            this.add(Items.GLOWSTONE.getTranslationKey());
            this.add(Items.RESPAWN_ANCHOR.getTranslationKey());
            this.add(Items.ANVIL.getTranslationKey());
            return;
         }

         for (String s : IOUtils.readLines(new FileInputStream(friendFile), StandardCharsets.UTF_8)) {
            this.add(s);
         }
      } catch (Exception var5) {
         var5.printStackTrace();
      }
   }

   public void save() {
      try {
         File friendFile = getFile("cleaner.txt");
         PrintWriter printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(friendFile), StandardCharsets.UTF_8));

         for (String str : this.list) {
            printwriter.println(str);
         }

         printwriter.close();
      } catch (Exception var5) {
         var5.printStackTrace();
      }
   }
}
