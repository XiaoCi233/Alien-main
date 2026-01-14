package dev.luminous.api.interfaces;

import dev.luminous.api.utils.math.FadeUtils;

public interface IChatHudLineHook {
   int alienClient$getMessageId();

   void alienClient$setMessageId(int var1);

   boolean alienClient$getSync();

   void alienClient$setSync(boolean var1);

   FadeUtils alienClient$getFade();

   void alienClient$setFade(FadeUtils var1);
}
