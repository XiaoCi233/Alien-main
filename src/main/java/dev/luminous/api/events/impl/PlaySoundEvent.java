package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import net.minecraft.client.sound.SoundInstance;

public class PlaySoundEvent extends Event {
   private static final PlaySoundEvent INSTANCE = new PlaySoundEvent();
   public SoundInstance sound;

   public static PlaySoundEvent get(SoundInstance sound) {
      INSTANCE.setCancelled(false);
      INSTANCE.sound = sound;
      return INSTANCE;
   }
}
