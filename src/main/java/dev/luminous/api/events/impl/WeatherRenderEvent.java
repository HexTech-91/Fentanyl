package dev.luminous.api.events.impl;

import dev.luminous.api.events.eventbus.ICancellable;
import net.minecraft.client.render.LightmapTextureManager;

public class WeatherRenderEvent implements ICancellable {
   private static final WeatherRenderEvent INSTANCE = new WeatherRenderEvent();
   public LightmapTextureManager lightmapTextureManager;
   public float tickDelta;
   public double cameraX;
   public double cameraY;
   public double cameraZ;
   private boolean cancelled;

   public static WeatherRenderEvent get(LightmapTextureManager var0, float var1, double var2, double var4, double var6) {
      INSTANCE.setCancelled(false);
      INSTANCE.lightmapTextureManager = var0;
      INSTANCE.tickDelta = var1;
      INSTANCE.cameraX = var2;
      INSTANCE.cameraY = var4;
      INSTANCE.cameraZ = var6;
      return INSTANCE;
   }

   @Override
   public boolean isCancelled() {
      return this.cancelled;
   }

   @Override
   public void setCancelled(boolean var1) {
      this.cancelled = var1;
   }
}
