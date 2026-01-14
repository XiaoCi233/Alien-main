package dev.luminous.api.events.eventbus;

public interface IListener {
   void call(Object var1);

   Class<?> getTarget();

   int getPriority();

   boolean isStatic();
}
