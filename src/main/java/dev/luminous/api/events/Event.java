package dev.luminous.api.events;

public class Event {
   public Event.Stage stage;
   private boolean cancel = false;

   public Event() {
      this(Event.Stage.Pre);
   }

   public Event(Event.Stage stage) {
      this.stage = stage;
   }

   public void cancel() {
      this.setCancelled(true);
   }

   public boolean isCancelled() {
      return this.cancel;
   }

   public void setCancelled(boolean cancel) {
      this.cancel = cancel;
   }

   public boolean isPost() {
      return this.stage == Event.Stage.Post;
   }

   public boolean isPre() {
      return this.stage == Event.Stage.Pre;
   }

   public static enum Stage {
      Pre,
      Post;
   }
}
