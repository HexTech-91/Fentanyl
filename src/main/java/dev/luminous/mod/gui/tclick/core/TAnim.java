package dev.luminous.mod.gui.tclick.core;

import net.minecraft.client.MinecraftClient;

public final class TAnim {
   private TAnim() {
   }

   public static float deltaTime() {
      return TFps.get() > 5 ? 1.0F / TFps.get() : 0.016F;
   }

   public static float fast(float var0, float var1, float var2) {
      float var3 = clamp(deltaTime() * var2, 0.0F, 1.0F);
      return (1.0F - var3) * var0 + var3 * var1;
   }

   public static double interpolate(double var0, double var2, double var4) {
      return var2 + (var0 - var2) * var4;
   }

   public static float interpolateFloat(float var0, float var1, double var2) {
      return (float)(var0 + (var1 - var0) * var2);
   }

   public static float scrollAnimate(float var0, float var1, float var2) {
      boolean var3 = var0 > var1;
      if (var2 < 0.0F) {
         var2 = 0.0F;
      } else if (var2 > 1.0F) {
         var2 = 1.0F;
      }

      float var4 = Math.max(var0, var1) - Math.min(var0, var1);
      float var5 = var4 * var2;
      return var1 + (var3 ? var5 : -var5);
   }

   public static float tickDelta() {
      try {
         MinecraftClient var0 = MinecraftClient.getInstance();
         return var0 != null && var0.getRenderTickCounter() != null ? var0.getRenderTickCounter().getTickDelta(true) : 1.0F;
      } catch (Throwable var1) {
         return 1.0F;
      }
   }

   private static float clamp(float var0, float var1, float var2) {
      return var0 < var1 ? var1 : Math.min(var0, var2);
   }
}
