package dev.luminous.mod.modules.settings.impl;

import dev.luminous.mod.modules.settings.Setting;
import java.lang.reflect.Field;
import java.util.function.BooleanSupplier;
import org.lwjgl.glfw.GLFW;

public class BindSetting extends Setting {
   private final int defaultValue;
   public boolean holding = false;
   private int value;
   private boolean pressed = false;
   private boolean holdEnable = false;

   public BindSetting(String name, int value) {
      super(name);
      this.defaultValue = value;
      this.value = value;
   }

   public BindSetting(String name, int value, BooleanSupplier visibilityIn) {
      super(name, visibilityIn);
      this.defaultValue = value;
      this.value = value;
   }

   public int getValue() {
      return this.value;
   }

   public void setValue(int value) {
      this.value = value;
   }

   public String getKeyString() {
      if (this.value == -1) {
         return "None";
      } else if (this.value < -1) {
         return "Mouse" + (Math.abs(this.value) - 1);
      } else {
         String kn = this.value > 0 ? GLFW.glfwGetKeyName(this.value, GLFW.glfwGetKeyScancode(this.value)) : "None";
         if (kn == null) {
            try {
               for (Field declaredField : GLFW.class.getDeclaredFields()) {
                  if (declaredField.getName().startsWith("GLFW_KEY_")) {
                     int a = (Integer)declaredField.get(null);
                     if (a == this.value) {
                        String nb = declaredField.getName().substring("GLFW_KEY_".length());
                        kn = nb.substring(0, 1).toUpperCase() + nb.substring(1).toLowerCase();
                     }
                  }
               }
            } catch (Exception var8) {
               kn = "None";
            }
         }

         return kn == null ? "Unknown " + this.value : kn.toUpperCase();
      }
   }

   public boolean isPressed() {
      return this.pressed;
   }

   public void setPressed(boolean pressed) {
      this.pressed = pressed;
   }

   public boolean isHoldEnable() {
      return this.holdEnable;
   }

   public void setHoldEnable(boolean holdEnable) {
      this.holdEnable = holdEnable;
   }

   public int getDefaultValue() {
      return this.defaultValue;
   }
}
