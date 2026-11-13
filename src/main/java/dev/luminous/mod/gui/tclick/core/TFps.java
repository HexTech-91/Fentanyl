package dev.luminous.mod.gui.tclick.core;

import net.minecraft.client.MinecraftClient;

public final class TFps {
   private TFps() {
   }

   public static int get() {
      try {
         MinecraftClient var0 = MinecraftClient.getInstance();
         return var0 == null ? 5 : var0.getCurrentFps();
      } catch (Throwable var1) {
         return 5;
      }
   }
}
