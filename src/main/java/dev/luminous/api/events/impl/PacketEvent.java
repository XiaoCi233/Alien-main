package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import dev.luminous.api.events.Event;
import net.minecraft.network.packet.Packet;

public class PacketEvent extends Event {
   private final Packet<?> packet;

   public PacketEvent(Packet<?> packet, Event.Stage stage) {
      super(stage);
      this.packet = packet;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }

   public static class Receive extends PacketEvent {
      public Receive(Packet<?> packet) {
         super(packet, Event.Stage.Pre);
      }
   }

   public static class Send extends PacketEvent {
      public Send(Packet<?> packet) {
         super(packet, Event.Stage.Pre);
      }
   }

   public static class Sent extends PacketEvent {
      public Sent(Packet<?> packet) {
         super(packet, Event.Stage.Post);
      }
   }
}
