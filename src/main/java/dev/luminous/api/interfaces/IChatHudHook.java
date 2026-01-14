package dev.luminous.api.interfaces;

import net.minecraft.text.Text;

public interface IChatHudHook {
   void alienClient$addMessage(Text var1, int var2);

   void alienClient$addMessage(Text var1);

   void alienClient$addMessageOutSync(Text var1, int var2);
}
