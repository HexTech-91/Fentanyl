package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;
import java.awt.Color;

public class TotemParticleEvent extends Event {
   private static final TotemParticleEvent instance = new TotemParticleEvent();
   public double velocityX;
   public double velocityY;
   public double velocityZ;
   public Color color;

   private TotemParticleEvent() {
   }

   public static TotemParticleEvent get(double var0, double var2, double var4) {
      instance.velocityX = var0;
      instance.velocityY = var2;
      instance.velocityZ = var4;
      return instance;
   }
}
