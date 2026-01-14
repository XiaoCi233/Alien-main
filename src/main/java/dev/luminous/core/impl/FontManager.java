package dev.luminous.core.impl;

import dev.luminous.mod.gui.fonts.FontRenderer;
import dev.luminous.mod.modules.impl.client.Fonts;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import net.minecraft.client.util.math.MatrixStack;

public class FontManager {
   public static FontRenderer ui;
   public static FontRenderer small;
   public static FontRenderer icon;

   public static void init() {
      try {
         ui = assets(8.0F, "default", 0);
         small = assets(6.0F, "default", 0);
         icon = assetsWithoutOffset(8.0F, "icon", 0);
      } catch (Exception var1) {
         var1.printStackTrace();
      }
   }

   public static FontRenderer assets(float size, String font, int style, String alternate) throws IOException, FontFormatException {
      return new FontRenderer(
         Font.createFont(
               0, (InputStream)Objects.requireNonNull(FontManager.class.getClassLoader().getResourceAsStream("assets/alienclient/font/" + font + ".ttf"))
            )
            .deriveFont(style, size),
         getFont(alternate, style, (int)size),
         size
      ) {
         @Override
         public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a, boolean shadow) {
            super.drawString(stack, s, x + Fonts.INSTANCE.translate.getValueInt(), y + Fonts.INSTANCE.shift.getValueInt(), r, g, b, a, shadow);
         }
      };
   }

   public static FontRenderer assetsWithoutOffset(float size, String name, int style) throws IOException, FontFormatException {
      return new FontRenderer(
         Font.createFont(
               0, (InputStream)Objects.requireNonNull(FontManager.class.getClassLoader().getResourceAsStream("assets/alienclient/font/" + name + ".ttf"))
            )
            .deriveFont(style, size),
         size
      );
   }

   public static FontRenderer assets(float size, String name, int style) throws IOException, FontFormatException {
      return new FontRenderer(
         Font.createFont(
               0, (InputStream)Objects.requireNonNull(FontManager.class.getClassLoader().getResourceAsStream("assets/alienclient/font/" + name + ".ttf"))
            )
            .deriveFont(style, size),
         size
      ) {
         @Override
         public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a, boolean shadow) {
            super.drawString(stack, s, x + Fonts.INSTANCE.translate.getValueInt(), y + Fonts.INSTANCE.shift.getValueInt(), r, g, b, a, shadow);
         }
      };
   }

   public static FontRenderer create(int size, String font, int style, String alternate) {
      return new FontRenderer(getFont(font, style, size), getFont(alternate, style, size), size) {
         @Override
         public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a, boolean shadow) {
            super.drawString(stack, s, x + Fonts.INSTANCE.translate.getValueInt(), y + Fonts.INSTANCE.shift.getValueInt(), r, g, b, a, shadow);
         }
      };
   }

   public static FontRenderer create(int size, String font, int style) {
      return new FontRenderer(getFont(font, style, size), size) {
         @Override
         public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a, boolean shadow) {
            super.drawString(stack, s, x + Fonts.INSTANCE.translate.getValueInt(), y + Fonts.INSTANCE.shift.getValueInt(), r, g, b, a, shadow);
         }
      };
   }

   private static Font getFont(String font, int style, int size) {
      File fontDir = new File("C:\\Windows\\Fonts");

      try {
         for (File file : fontDir.listFiles()) {
            if (file.getName().replace(".ttf", "").replace(".ttc", "").replace(".otf", "").equalsIgnoreCase(font)) {
               try {
                  return Font.createFont(0, file).deriveFont(style, size);
               } catch (Exception var9) {
                  var9.printStackTrace();
               }
            }
         }

         for (File filex : fontDir.listFiles()) {
            if (filex.getName().startsWith(font)) {
               try {
                  return Font.createFont(0, filex).deriveFont(style, size);
               } catch (Exception var10) {
                  var10.printStackTrace();
               }
            }
         }
      } catch (Exception var11) {
      }

      return new Font(null, style, size);
   }
}
