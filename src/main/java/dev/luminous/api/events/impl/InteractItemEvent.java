package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import dev.luminous.api.events.Event;
import net.minecraft.util.Hand;

public class InteractItemEvent extends Event {
   private static final InteractItemEvent INSTANCE = new InteractItemEvent();
   public Hand hand;

   private InteractItemEvent() {
   }

   public static InteractItemEvent getPre(Hand hand) {
      INSTANCE.hand = hand;
      INSTANCE.stage = Event.Stage.Pre;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }

   public static InteractItemEvent getPost(Hand hand) {
      INSTANCE.hand = hand;
      INSTANCE.stage = Event.Stage.Post;
      return INSTANCE;
   }
}
