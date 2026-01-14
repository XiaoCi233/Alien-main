package dev.luminous.mod.modules.impl.client;

import dev.luminous.core.impl.FontManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;

public class Fonts extends Module {
   public static Fonts INSTANCE;
   public final StringSetting font = this.add(new StringSetting("Font", "default"));
   public final StringSetting alternate = this.add(new StringSetting("Alternate", "msyh"));
   public final EnumSetting<Fonts.Style> style = this.add(new EnumSetting("Style", Fonts.Style.PLAIN));
   public final SliderSetting size = this.add(new SliderSetting("Size", 8.0, 1.0, 15.0, 1.0));
   public final SliderSetting shift = this.add(new SliderSetting("Shift", 0.0, -10.0, 10.0, 1.0));
   public final SliderSetting translate = this.add(new SliderSetting("Translate", 0.0, -10.0, 10.0, 1.0));

   public Fonts() {
      super("Fonts", Module.Category.Client);
      this.setChinese("字体");
      INSTANCE = this;
   }

   @Override
   public void enable() {
      this.refresh();
   }

   public void refresh() {
      try {
         if (this.font.getValue().equals("default")) {
            if (this.alternate.getValue().equals("null")) {
               FontManager.ui = FontManager.assets(this.size.getValueInt(), this.font.getValue(), ((Fonts.Style)this.style.getValue()).get());
               FontManager.small = FontManager.assets(6.0F, this.font.getValue(), ((Fonts.Style)this.style.getValue()).get());
            } else {
               FontManager.ui = FontManager.assets(
                  this.size.getValueInt(), this.font.getValue(), ((Fonts.Style)this.style.getValue()).get(), this.alternate.getValue()
               );
               FontManager.small = FontManager.assets(6.0F, this.font.getValue(), ((Fonts.Style)this.style.getValue()).get(), this.alternate.getValue());
            }
         } else if (this.alternate.getValue().equals("null")) {
            FontManager.ui = FontManager.create(this.size.getValueInt(), this.font.getValue(), ((Fonts.Style)this.style.getValue()).get());
            FontManager.small = FontManager.create(6, this.font.getValue(), ((Fonts.Style)this.style.getValue()).get());
         } else {
            FontManager.ui = FontManager.create(
               this.size.getValueInt(), this.font.getValue(), ((Fonts.Style)this.style.getValue()).get(), this.alternate.getValue()
            );
            FontManager.small = FontManager.create(6, this.font.getValue(), ((Fonts.Style)this.style.getValue()).get(), this.alternate.getValue());
         }
      } catch (Exception var2) {
         var2.printStackTrace();
      }
   }

   public static enum Style {
      PLAIN {
         @Override
         public int get() {
            return 0;
         }
      },
      BOLD {
         @Override
         public int get() {
            return 1;
         }
      },
      ITALIC {
         @Override
         public int get() {
            return 2;
         }
      };

      public abstract int get();
   }
}
