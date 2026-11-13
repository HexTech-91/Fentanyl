package dev.luminous.api.utils.render;

public final class AlienFrameStamp {
   public static volatile long value = 0L;

   private AlienFrameStamp() {
   }

   public static void next() {
      value++;
   }
}
