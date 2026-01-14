package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import dev.luminous.api.events.Event;
import net.minecraft.util.Hand;

public class InteractBlockEvent extends Event {
   private static final InteractBlockEvent INSTANCE = new InteractBlockEvent();
   public Hand hand;

   private InteractBlockEvent() {
   }

   public static InteractBlockEvent getPre(Hand hand) {
      INSTANCE.hand = hand;
      INSTANCE.stage = Event.Stage.Pre;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }

   public static InteractBlockEvent getPost(Hand hand) {
      INSTANCE.hand = hand;
      INSTANCE.stage = Event.Stage.Post;
      return INSTANCE;
   }
}
