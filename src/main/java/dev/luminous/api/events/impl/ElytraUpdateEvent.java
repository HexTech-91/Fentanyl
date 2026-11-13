package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;

public class ElytraUpdateEvent extends Event {
   private static final ElytraUpdateEvent instance = new ElytraUpdateEvent();

   private ElytraUpdateEvent() {
   }

   public static ElytraUpdateEvent get() {
      instance.setCancelled(false);
      return instance;
   }
}
