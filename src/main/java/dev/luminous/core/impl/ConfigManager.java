package dev.luminous.core.impl;

import com.google.common.base.Splitter;
import dev.luminous.Alien;
import dev.luminous.core.Manager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.HUD;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;

public class ConfigManager extends Manager {
   public static File options = getFile("options.txt");
   private final Hashtable<String, String> settings = new Hashtable();

   public ConfigManager() {
      this.read();
   }

   
   public void load() {
      for (Module module : Alien.MODULE.getModules()) {
         for (Setting setting : module.getSettings()) {
            String line = module.getName() + "_" + setting.getName();
            if (setting instanceof BooleanSetting s){
                s.setValueWithoutTask(Alien.CONFIG.getBoolean(line, s.getDefaultValue()));
            } else if (setting instanceof SliderSetting sx) {
                 sx.setValue(Alien.CONFIG.getFloat(line, (float)sx.getDefaultValue()));
            } else if (setting instanceof BindSetting sxx) {
                sxx.setValue(Alien.CONFIG.getInt(line, sxx.getDefaultValue()));
                sxx.setHoldEnable(Alien.CONFIG.getBoolean(line + "_hold"));
            } else if (setting instanceof EnumSetting<?> sxxx) {
                sxxx.loadSetting(Alien.CONFIG.getString(line));
            } else if (setting instanceof ColorSetting sxxxx) {
                sxxxx.setValue(new Color(Alien.CONFIG.getInt(line, sxxxx.getDefaultValue().getRGB()), true));
                sxxxx.setSync(Alien.CONFIG.getBoolean(line + "Sync", sxxxx.getDefaultSync()));
                if (sxxxx.injectBoolean) {
                    sxxxx.booleanValue = Alien.CONFIG.getBoolean(line + "Boolean", sxxxx.getDefaultBooleanValue());
                }
            } else if (setting instanceof StringSetting sxxxxx) {
                sxxxxx.setValue(Alien.CONFIG.getString(line, sxxxxx.getDefaultValue()));
            }
         }

         module.setState(Alien.CONFIG.getBoolean(module.getName() + "_state", module instanceof HUD));
      }
   }

   public void save() {
      PrintWriter printwriter = null;

      try {
         printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(options), StandardCharsets.UTF_8));

         for (Module module : Alien.MODULE.getModules()) {
            for (Setting setting : module.getSettings()) {
               String line = module.getName() + "_" + setting.getName();
               if (setting instanceof BooleanSetting s){
                   printwriter.println(line + ":" + s.getValue());
               } else if (setting instanceof SliderSetting sx) {
                   printwriter.println(line + ":" + sx.getValue());
               } else if (setting instanceof BindSetting sxx) {
                   printwriter.println(line + ":" + sxx.getValue());
                   printwriter.println(line + "_hold:" + sxx.isHoldEnable());
               } else if (setting instanceof EnumSetting<?> sxxx) {
                   printwriter.println(line + ":" + sxxx.getValue().name());
               } else if (setting instanceof ColorSetting sxxxx) {
                   printwriter.println(line + ":" + sxxxx.getValue().getRGB());
                   printwriter.println(line + "Sync:" + sxxxx.sync);
                   if (sxxxx.injectBoolean) {
                       printwriter.println(line + "Boolean:" + sxxxx.booleanValue);
                   }
               } else if (setting instanceof StringSetting sxxxxx) {
                   printwriter.println(line + ":" + sxxxxx.getValue());
               }
            }

            printwriter.println(module.getName() + "_state:" + module.isOn());
         }
      } catch (Exception var18) {
         var18.printStackTrace();
         System.out.println("[Alien] Failed to save settings");
      } finally {
         IOUtils.closeQuietly(printwriter);
      }
   }

   
   public void read() {
      Splitter COLON_SPLITTER = Splitter.on(':');

      try {
         if (!options.exists()) {
            return;
         }

         for (String s : IOUtils.readLines(new FileInputStream(options), StandardCharsets.UTF_8)) {
            try {
               Iterator<String> iterator = COLON_SPLITTER.limit(2).split(s).iterator();
               this.settings.put((String)iterator.next(), (String)iterator.next());
            } catch (Exception var6) {
               System.out.println("Skipping bad option: " + s);
            }
         }
      } catch (Exception var7) {
         var7.printStackTrace();
         System.out.println("[Alien] Failed to load settings");
      }
   }

   public int getInt(String setting, int defaultValue) {
      String s = (String)this.settings.get(setting);
      return s != null && this.isInteger(s) ? Integer.parseInt(s) : defaultValue;
   }

   public float getFloat(String setting, float defaultValue) {
      String s = (String)this.settings.get(setting);
      return s != null && this.isFloat(s) ? Float.parseFloat(s) : defaultValue;
   }

   public boolean getBoolean(String setting) {
      String s = (String)this.settings.get(setting);
      return Boolean.parseBoolean(s);
   }

   public boolean getBoolean(String setting, boolean defaultValue) {
      if (this.settings.get(setting) != null) {
         String s = (String)this.settings.get(setting);
         return Boolean.parseBoolean(s);
      } else {
         return defaultValue;
      }
   }

   public String getString(String setting) {
      return (String)this.settings.get(setting);
   }

   public String getString(String setting, String defaultValue) {
      return this.settings.get(setting) == null ? defaultValue : (String)this.settings.get(setting);
   }

   public boolean isInteger(String str) {
      Pattern pattern = Pattern.compile("^[-+]?[\\d]*$");
      return pattern.matcher(str).matches();
   }

   public boolean isFloat(String str) {
      String pattern = "^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$";
      return str.matches(pattern);
   }
}
