package dev.luminous.mod.modules.settings;

public class EnumConverter {
   public static int currentEnum(Enum<?> clazz) {
      for (int i = 0; i < ((Enum[])clazz.getDeclaringClass().getEnumConstants()).length; i++) {
         Enum<?> e = ((Enum[])clazz.getDeclaringClass().getEnumConstants())[i];
         if (e.name().equalsIgnoreCase(clazz.name())) {
            return i;
         }
      }

      return -1;
   }

   public static Enum<?> increaseEnum(Enum<?> clazz) {
      int index = currentEnum(clazz);

      for (int i = 0; i < ((Enum[])clazz.getDeclaringClass().getEnumConstants()).length; i++) {
         if (i == index + 1) {
            return ((Enum[])clazz.getDeclaringClass().getEnumConstants())[i];
         }
      }

      return ((Enum[])clazz.getDeclaringClass().getEnumConstants())[0];
   }

   public Enum<?> get(Enum<?> clazz, String string) {
      try {
         for (int i = 0; i < ((Enum[])clazz.getDeclaringClass().getEnumConstants()).length; i++) {
            Enum<?> e = ((Enum[])clazz.getDeclaringClass().getEnumConstants())[i];
            if (e.name().equalsIgnoreCase(string)) {
               return e;
            }
         }

         return null;
      } catch (IllegalArgumentException var5) {
         return null;
      }
   }
}
