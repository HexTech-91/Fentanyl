package dev.luminous.api.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.particle.TotemParticle;

public final class PerformanceLimiter {
   private static final String TOTEM_MODULE_CLASS = "dev.luminous.mod.modules.impl.render.TotemParticle";
   private static final int TOTEM_PARTICLES_PER_WINDOW = 32;
   private static final long WINDOW_MS = 100L;
   private static final long MODULE_REFRESH_MS = 250L;
   private static long particleWindowStart;
   private static int particleWindowCount;
   private static long moduleCheckedAt;
   private static boolean moduleEnabled;
   private static Class<?> totemParticleClass;
   private static Method moduleIsOn;
   private static Field moduleInstance;

   private PerformanceLimiter() {
   }

   public static boolean allowParticle(Object var0) {
      if (var0 != null && isTotemParticle(var0) && isTotemModuleEnabled()) {
         long var1 = System.nanoTime() / 1000000L;
         synchronized (PerformanceLimiter.class) {
            if (var1 - particleWindowStart >= 100L) {
               particleWindowStart = var1;
               particleWindowCount = 0;
            }

            return particleWindowCount++ < 32;
         }
      } else {
         return true;
      }
   }

   public static int clampParticles(int var0) {
      return Math.max(0, Math.min(256, var0));
   }

   public static int clampFireflies(int var0) {
      return Math.max(0, Math.min(80, var0));
   }

   private static boolean isTotemModuleEnabled() {
      long var0 = System.nanoTime() / 1000000L;
      if (var0 - moduleCheckedAt < 250L) {
         return moduleEnabled;
      } else {
         synchronized (PerformanceLimiter.class) {
            if (var0 - moduleCheckedAt < 250L) {
               return moduleEnabled;
            } else {
               moduleCheckedAt = var0;

               try {
                  if (moduleInstance == null || moduleIsOn == null) {
                     Class var3 = Class.forName("dev.luminous.mod.modules.impl.render.TotemParticle");
                     moduleInstance = var3.getField("INSTANCE");
                     moduleIsOn = var3.getMethod("isOn");
                  }

                  Object var7 = moduleInstance.get(null);
                  moduleEnabled = var7 != null && Boolean.TRUE.equals(moduleIsOn.invoke(var7));
               } catch (Throwable var5) {
                  moduleEnabled = false;
               }

               return moduleEnabled;
            }
         }
      }
   }

   private static boolean isTotemParticle(Object var0) {
      Class var1 = totemParticleClass;
      if (var1 == null) {
         synchronized (PerformanceLimiter.class) {
            var1 = totemParticleClass;
            if (var1 == null) {
               try {
                  var1 = TotemParticle.class;
                  totemParticleClass = var1;
               } catch (Throwable var5) {
                  return false;
               }
            }
         }
      }

      return var1.isInstance(var0);
   }
}
