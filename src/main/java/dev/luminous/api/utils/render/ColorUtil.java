package dev.luminous.api.utils.render;

import java.awt.Color;

public class ColorUtil {
   public static Color fadeColor(Color startColor, Color endColor, double progress) {
      progress = Math.min(Math.max(progress, 0.0), 1.0);
      int sR = startColor.getRed();
      int sG = startColor.getGreen();
      int sB = startColor.getBlue();
      int sA = startColor.getAlpha();
      int eR = endColor.getRed();
      int eG = endColor.getGreen();
      int eB = endColor.getBlue();
      int eA = endColor.getAlpha();
      return new Color(
         Math.min((int)(sR + (eR - sR) * progress), 255),
         Math.min((int)(sG + (eG - sG) * progress), 255),
         Math.min((int)(sB + (eB - sB) * progress), 255),
         Math.min((int)(sA + (eA - sA) * progress), 255)
      );
   }

   public static Color hslToColor(float f, float f2, float f3, float f4) {
      if (f2 < 0.0F || f2 > 100.0F) {
         throw new IllegalArgumentException("Color parameter outside of expected range - Saturation");
      } else if (f3 < 0.0F || f3 > 100.0F) {
         throw new IllegalArgumentException("Color parameter outside of expected range - Lightness");
      } else if (!(f4 < 0.0F) && !(f4 > 1.0F)) {
         f %= 360.0F;
         float var8;
         float f5 = f3 < 0.5 ? f3 * (1.0F + f2) : (f3 /= 100.0F) + (var8 = f2 / 100.0F) - var8 * f3;
         f2 = 2.0F * f3 - f5;
         float var7;
         f3 = Math.max(0.0F, colorCalc(f2, f5, (var7 = f / 360.0F) + 0.33333334F));
         float f6 = Math.max(0.0F, colorCalc(f2, f5, var7));
         f2 = Math.max(0.0F, colorCalc(f2, f5, var7 - 0.33333334F));
         f3 = Math.min(f3, 1.0F);
         f6 = Math.min(f6, 1.0F);
         f2 = Math.min(f2, 1.0F);
         return new Color(f3, f6, f2, f4);
      } else {
         throw new IllegalArgumentException("Color parameter outside of expected range - Alpha");
      }
   }

   private static float colorCalc(float f, float f2, float f3) {
      if (f3 < 0.0F) {
         f3++;
      }

      if (f3 > 1.0F) {
         f3--;
      }

      if (6.0F * f3 < 1.0F) {
         return f + (f2 - f) * 6.0F * f3;
      } else if (2.0F * f3 < 1.0F) {
         return f2;
      } else {
         return 3.0F * f3 < 2.0F ? f + (f2 - f) * 6.0F * (0.6666667F - f3) : f;
      }
   }

   public static Color injectAlpha(Color color, int alpha) {
      alpha = Math.max(Math.min(255, alpha), 0);
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
   }

   public static int injectAlpha(int color, int alpha) {
      return color & 16777215 | alpha << 24;
   }

   public static Color pulseColor(Color startColor, Color endColor, double index, int count, double speed) {
      double brightness = Math.abs(
         (
                  System.currentTimeMillis() * speed % 2000.0 / Float.intBitsToFloat(Float.floatToIntBits(0.0013786979F) ^ 2127476077)
                     + index / count * Float.intBitsToFloat(Float.floatToIntBits(0.09192204F) ^ 2109489567)
               )
               % Float.intBitsToFloat(Float.floatToIntBits(0.7858098F) ^ 2135501525)
            - Float.intBitsToFloat(Float.floatToIntBits(6.46708F) ^ 2135880274)
      );
      double quad = brightness % Float.intBitsToFloat(Float.floatToIntBits(0.8992331F) ^ 2137404452);
      return fadeColor(startColor, endColor, quad);
   }

   public static Color pulseColor(Color color, double index, int count, double speed) {
      float[] hsb = new float[3];
      Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
      double brightness = Math.abs(
         (
                  System.currentTimeMillis() * speed % 2000.0 / Float.intBitsToFloat(Float.floatToIntBits(0.0013786979F) ^ 2127476077)
                     + index / count * Float.intBitsToFloat(Float.floatToIntBits(0.09192204F) ^ 2109489567)
               )
               % Float.intBitsToFloat(Float.floatToIntBits(0.7858098F) ^ 2135501525)
            - Float.intBitsToFloat(Float.floatToIntBits(6.46708F) ^ 2135880274)
      );
      brightness = Float.intBitsToFloat(Float.floatToIntBits(18.996923F) ^ 2123889075)
         + Float.intBitsToFloat(Float.floatToIntBits(2.7958195F) ^ 2134044341) * brightness;
      hsb[2] = (float)(brightness % Float.intBitsToFloat(Float.floatToIntBits(0.8992331F) ^ 2137404452));
      return new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
   }
}
