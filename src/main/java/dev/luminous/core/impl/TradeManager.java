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

public class TradeManager extends Manager {
   private final ArrayList<String> list = new ArrayList();

   public TradeManager() {
      this.read();
   }

   public ArrayList<String> getList() {
      return this.list;
   }

   public void clear() {
      this.list.clear();
   }

   public boolean inWhitelist(String name) {
      return this.list.contains(name) || this.list.contains(name.replace("block.minecraft.", "").replace("item.minecraft.", ""));
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
         File friendFile = getFile("trades.txt");
         if (!friendFile.exists()) {
            this.add(Items.ENCHANTED_BOOK.getTranslationKey());
            this.add(Items.DIAMOND_BLOCK.getTranslationKey());
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
         File friendFile = getFile("trades.txt");
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
